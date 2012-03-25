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

import gnu.trove.iterator.TLongIterator;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.set.hash.TLongHashSet;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.gridfs.GridFS;

import de.fhg.igd.mongomvcc.VBranch;
import de.fhg.igd.mongomvcc.VCollection;
import de.fhg.igd.mongomvcc.VCounter;
import de.fhg.igd.mongomvcc.VException;
import de.fhg.igd.mongomvcc.VLargeCollection;
import de.fhg.igd.mongomvcc.impl.internal.Commit;
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
	 * The MongoDB database object
	 */
	private final DB _db;
	
	/**
	 * A counter to generate unique IDs
	 */
	private final VCounter _counter;
	
	/**
	 * Constructs a new branch object (not the branch itself)
	 * @param name the branch's name (may be null for unnamed branches)
	 * @param rootCid the CID of the branch's root
	 * @param tree the tree of commits
	 * @param db the MongoDB database object
	 * @param counter a counter to generate unique IDs
	 */
	public MongoDBVBranch(String name, long rootCid, Tree tree, DB db, VCounter counter) {
		_name = name;
		_rootCid = rootCid;
		_tree = tree;
		_db = db;
		_counter = counter;
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
	
	/**
	 * Updates the thread-local head of the currently checked out branch/commit
	 * @param newHead the new head
	 */
	private void updateHead(Commit newHead) {
		_head.set(newHead);
	}
	
	@Override
	public long getHead() {
		return getHeadCommit().getCID();
	}
	
	@Override
	public VCollection getCollection(String name) {
		return new MongoDBVCollection(_db.getCollection(name), this, _counter);
	}
	
	@Override
	public VLargeCollection getLargeCollection(String name) {
		return new MongoDBVLargeCollection(_db.getCollection(name), new GridFS(_db, name), this, _counter);
	}
	
	/**
	 * Gets or creates a database collection that can handle large
	 * objects (BLOBs) and uses a given access strategy.
	 * @param name the collection's name
	 * @param accessStrategy the strategy used to access large objects
	 * @return the collection (never null)
	 */
	public VLargeCollection getLargeCollection(String name, AccessStrategy accessStrategy) {
		return new MongoDBVLargeCollection(_db.getCollection(name), new GridFS(_db, name),
				this, _counter, accessStrategy);
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
	 * @return the CID of this branch's root
	 */
	public long getRootCid() {
		return _rootCid;
	}
	
	@Override
	public long commit() {
		Index idx = getIndex();
		//clone dirty objects because we clear them below
		Map<String, TLongLongHashMap> dos = new HashMap<String, TLongLongHashMap>(idx.getDirtyObjects());
		Commit head = getHeadCommit();
		Commit c = new Commit(_counter.getNextId(), head.getCID(), _rootCid, dos);
		_tree.addCommit(c);
		updateHead(c);
		
		//mark deleted objects as deleted in the database
		String lifetimeAttr = "_lifetime." + getRootCid();
		for (Map.Entry<String, TLongHashSet> e : idx.getDeletedOids().entrySet()) {
			DBCollection dbc = _db.getCollection(e.getKey());
			TLongIterator li = e.getValue().iterator();
			while (li.hasNext()) {
				long oid = li.next();
				dbc.update(new BasicDBObject(MongoDBConstants.ID, oid), new BasicDBObject("$set",
						new BasicDBObject(lifetimeAttr, head.getCID())));
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
