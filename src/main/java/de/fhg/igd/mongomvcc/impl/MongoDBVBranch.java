// This file is part of MongoMVCC.
//
// Copyright (c) 2012 Fraunhofer IGD
//
// MongoMVCC is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// MongoMVCC is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with MongoMVCC. If not, see <http://www.gnu.org/licenses/>.

package de.fhg.igd.mongomvcc.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.gridfs.GridFS;

import de.fhg.igd.mongomvcc.VBranch;
import de.fhg.igd.mongomvcc.VCollection;
import de.fhg.igd.mongomvcc.VException;
import de.fhg.igd.mongomvcc.VLargeCollection;
import de.fhg.igd.mongomvcc.helper.IdMap;
import de.fhg.igd.mongomvcc.helper.IdMapIterator;
import de.fhg.igd.mongomvcc.helper.IdSet;
import de.fhg.igd.mongomvcc.helper.IdSetIterator;
import de.fhg.igd.mongomvcc.impl.internal.Commit;
import de.fhg.igd.mongomvcc.impl.internal.CompatibilityHelper;
import de.fhg.igd.mongomvcc.impl.internal.Index;
import de.fhg.igd.mongomvcc.impl.internal.MongoDBConstants;
import de.fhg.igd.mongomvcc.impl.internal.Tree;

/**
 * <p>Implementation of {@link VBranch} for MongoDB. Each
 * thread owns an index and a head of the branch/commit it has checked out.
 * This ensures isolation. If two threads try to create a commit with the
 * same head, the {@link #commit()} method will fail. The rule is: come first,
 * serve first.</p>
 * <p><strong>Thread-safety:</strong> this class is thread-safe. Instances
 * can be shared between threads, but each thread has its own index and head.</p>
 * @author Michel Kraemer
 */
public class MongoDBVBranch implements VBranch {
	/**
	 * The head of this branch. A thread-local variable since objects of
	 * this class can be shared between threads.
	 */
	private final ThreadLocal<Commit> _head = new ThreadLocal<Commit>();
	
	/**
	 * The index. Provides access to all objects from the currently
	 * checked out branch/commit and stores information on dirty objects.
	 */
	private final ThreadLocal<Index> _index = new ThreadLocal<Index>();
	
	/**
	 * Holds the query object for the current head. A query object is used to
	 * limit the number of objects transferred from the database.
	 */
	private final ThreadLocal<Map<String, Object>> _currentQueryObject =
			new ThreadLocal<Map<String, Object>>();
	
	/**
	 * The branch's name (may be null)
	 */
	private final String _name;
	
	/**
	 * The CID of the branch's root
	 */
	private final long _rootCid;
	
	/**
	 * The tree of commits
	 */
	private final Tree _tree;
	
	/**
	 * The database object
	 */
	private final MongoDBVDatabase _db;
	
	/**
	 * Constructs a new branch object (not the branch itself)
	 * @param name the branch's name (may be null for unnamed branches)
	 * @param rootCid the CID of the branch's root
	 * @param tree the tree of commits
	 * @param db the database object
	 */
	public MongoDBVBranch(String name, long rootCid, Tree tree, MongoDBVDatabase db) {
		_name = name;
		_rootCid = rootCid;
		_tree = tree;
		_db = db;
	}
	
	/**
	 * @return the thread-local head of this branch
	 */
	private Commit getHeadCommit() {
		Commit r = _head.get();
		if (r == null) {
			if (_name != null) {
				r = _tree.resolveBranch(_name);
			} else {
				r = _tree.resolveCommit(_rootCid);
			}
			_head.set(r);
		}
		return r;
	}
	
	private Map<String, Object> makeQueryObject() {
		BasicDBList l = new BasicDBList();
		String lba = MongoDBConstants.LIFETIME + "." + getRootCid();
		String iba = MongoDBConstants.LIFETIME + ".i" + getRootCid();
		
		//(1) check if there is information about the document's insertion
		//    available. if not just include this object.
		//    TODO this condition must be removed once we know the whole branching history
		l.add(new BasicDBObject(iba, new BasicDBObject("$exists", false)));
		
		if (!CompatibilityHelper.supportsAnd(getDB())) {
			//(2) check if the object has been deleted after this commit.
			//    we use a $not here, so the query will also return 'true' if
			//    the attribute is not set.
			l.add(new BasicDBObject(lba, new BasicDBObject("$not",
					new BasicDBObject("$lte", getHead()))));
		} else {
			BasicDBList l2 = new BasicDBList();
			//(2) check if the object has been inserted in this commit or later
			l2.add(new BasicDBObject(iba, new BasicDBObject("$lte", getHead())));
			
			//(3) check if the object has been deleted after this commit.
			//    we use a $not here, so the query will also return 'true' if
			//    the attribute is not set.
			l2.add(new BasicDBObject(lba, new BasicDBObject("$not",
					new BasicDBObject("$lte", getHead()))));
			
			l.add(new BasicDBObject("$and", l2));
		}
		
		return Collections.unmodifiableMap(new BasicDBObject("$or", l));
	}
	
	/**
	 * @return a query object which limits the number of objects returned
	 * by accessing their lifetime information
	 */
	public Map<String, Object> getQueryObject() {
		Map<String, Object> r = _currentQueryObject.get();
		if (r == null) {
			r = makeQueryObject();
			_currentQueryObject.set(r);
		}
		return r;
	}
	
	/**
	 * Updates the thread-local head of the currently checked out branch/commit
	 * @param newHead the new head
	 */
	private void updateHead(Commit newHead) {
		_currentQueryObject.remove();
		_head.set(newHead);
	}
	
	@Override
	public long getHead() {
		return getHeadCommit().getCID();
	}
	
	@Override
	public VCollection getCollection(String name) {
		return new MongoDBVCollection(_db.getDB().getCollection(name), this, _db.getCounter());
	}
	
	@Override
	public VLargeCollection getLargeCollection(String name) {
		DB db = _db.getDB();
		return new MongoDBVLargeCollection(db.getCollection(name),
				new GridFS(db, name), this, _db.getCounter());
	}
	
	/**
	 * Gets or creates a database collection that can handle large
	 * objects (BLOBs) and uses a given access strategy.
	 * @param name the collection's name
	 * @param accessStrategy the strategy used to access large objects
	 * @return the collection (never null)
	 */
	public VLargeCollection getLargeCollection(String name, AccessStrategy accessStrategy) {
		DB db = _db.getDB();
		return new MongoDBVLargeCollection(db.getCollection(name), new GridFS(db, name),
				this, _db.getCounter(), accessStrategy);
	}
	
	/**
	 * @return the index. The index provides access to all objects from
	 * the currently checked out branch/commit and stores information on
	 * dirty objects.
	 */
	public Index getIndex() {
		Index r = _index.get();
		if (r == null) {
			r = new Index(getHeadCommit(), _tree);
			_index.set(r);
		}
		return r;
	}
	
	/**
	 * @return the database from which this branch has been checked out
	 */
	public MongoDBVDatabase getDB() {
		return _db;
	}
	
	/**
	 * @return the CID of this branch's root
	 */
	public long getRootCid() {
		return _rootCid;
	}
	
	@Override
	public long commit() {
		Index idx = getIndex();
		//clone dirty objects because we clear them below
		Map<String, IdMap> dos = new HashMap<String, IdMap>(idx.getDirtyObjects());
		Commit head = getHeadCommit();
		Commit c = new Commit(_db.getCounter().getNextId(), head.getCID(), _rootCid, dos);
		_tree.addCommit(c);
		updateHead(c);
		
		//mark deleted objects as deleted in the database
		DB db = _db.getDB();
		String lifetimeAttr = MongoDBConstants.LIFETIME + "." + getRootCid();
		for (Map.Entry<String, IdSet> e : idx.getDeletedOids().entrySet()) {
			DBCollection dbc = db.getCollection(e.getKey());
			IdSetIterator li = e.getValue().iterator();
			while (li.hasNext()) {
				long oid = li.next();
				//save the CID of the commit where the object has been deleted
				dbc.update(new BasicDBObject(MongoDBConstants.ID, oid), new BasicDBObject("$set",
						new BasicDBObject(lifetimeAttr, head.getCID())));
			}
		}
		
		//mark dirty objects as inserted
		String instimeAttr = MongoDBConstants.LIFETIME + ".i" + getRootCid();
		for (Map.Entry<String, IdMap> e : dos.entrySet()) {
			DBCollection dbc = db.getCollection(e.getKey());
			IdMap m = e.getValue();
			IdMapIterator li = m.iterator();
			while (li.hasNext()) {
				li.advance();
				long oid = li.value();
				if (oid == -1) {
					//the document has been inserted and then deleted again
					//do not save time of insertion
					continue;
				}
				//save the CID of the commit where the object has been inserted
				dbc.update(new BasicDBObject(MongoDBConstants.ID, oid), new BasicDBObject("$set",
						new BasicDBObject(instimeAttr, head.getCID())));
			}
		}
		
		//reset index
		idx.clearDirtyObjects();
		
		//if we fail below, the commit has already been performed and the
		//index is clear. failing below simply means the named branch's
		//head could not be updated. If the caller wants to keep the commit
		//he/she just has to create a new named branch based on this
		//branch's head.
		
		//update named branch's head
		if (_name != null) {
			//synchronize the following part, because we first resolve the branch
			//and then update it
			synchronized (this) {
				//check for conflicts (i.e. if another thread has already updated the branch's head)
				if (_tree.resolveBranch(_name).getCID() != c.getParentCID()) {
					throw new VException("Branch " + _name + " has already been " +
							"updated by another commit");
				}
				_tree.updateBranchHead(_name, c.getCID());
			}
		}

		return c.getCID();
	}
	
	@Override
	public void rollback() {
		//simply reset the whole index
		_index.remove();
	}
}
