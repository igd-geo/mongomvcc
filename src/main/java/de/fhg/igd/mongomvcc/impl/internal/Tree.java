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

package de.fhg.igd.mongomvcc.impl.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;

import de.fhg.igd.mongomvcc.VException;
import de.fhg.igd.mongomvcc.VHistory;
import de.fhg.igd.mongomvcc.helper.IdHashMap;
import de.fhg.igd.mongomvcc.helper.IdMap;
import de.fhg.igd.mongomvcc.helper.IdMapIterator;

/**
 * <p>Represents the tree of commits.</p>
 * <p><strong>Thread-safety:</strong> This class is thread-safe.</p> 
 * @author Michel Kraemer
 */
public class Tree implements VHistory {
	/**
	 * Attribute names
	 */
	private final static String CID = "cid";
	private final static String ROOT_CID = "rootcid";
	private final static String PARENT_CID = "parent";
	private final static String OBJECTS = "objects";
	
	/**
     * Collection names
     */
    private final static String COLLECTION_BRANCHES = "_branches";
    private final static String COLLECTION_COMMITS = "_commits";
	
	/**
	 * A collection storing all branches and their current heads
	 */
	private final DBCollection _branches;
	
	/**
	 * A collection storing all commits (unsorted)
	 */
	private final DBCollection _commits;
	
	/**
	 * Creates a new tree object
	 * @param db the MongoDB database
	 */
	public Tree(DB db) {
		_branches = db.getCollection(COLLECTION_BRANCHES);
		_commits = db.getCollection(COLLECTION_COMMITS);
	}
	
	/**
	 * @return true if the tree is empty
	 */
	public boolean isEmpty() {
		return _commits.count() == 0;
	}
	
	/**
	 * Adds a commit to the tree
	 * @param commit the commit to add
	 */
	public void addCommit(Commit commit) {
		DBObject o = new BasicDBObject();
		o.put(MongoDBConstants.ID, commit.getCID());
		o.put(PARENT_CID, commit.getParentCID());
		o.put(ROOT_CID, commit.getRootCID());
		DBObject objs = new BasicDBObject();
		for (Map.Entry<String, IdMap> e : commit.getObjects().entrySet()) {
			DBObject co = new BasicDBObject();
			IdMapIterator it = e.getValue().iterator();
			while (it.hasNext()) {
				it.advance();
				co.put(String.valueOf(it.key()), it.value());
			}
			objs.put(e.getKey(), co);
		}
		o.put(OBJECTS, objs);
		_commits.insert(o);
	}
	
	/**
	 * Adds a named branch. Always waits for the database to fsync before
	 * returning. This guarantees all threads will see the change.
	 * @param name the branch's name
	 * @param headCID the CID of the head commit the branch points to
	 * @throws VException if there already is a branch with the given name or
	 * if the given head CID could not be resolved to an existing commit
	 */
	public void addBranch(String name, long headCID) {
		//synchronize here, because we first check for branch existence
		//and then we write
		synchronized(this) {
			//check prerequisites
			if (_branches.findOne(name) != null) {
				throw new VException("A branch with the name " + name + " already exists");
			}
			resolveCommit(headCID);
			
			//create branch
			DBObject o = new BasicDBObject();
			o.put(MongoDBConstants.ID, name);
			o.put(CID, headCID);
			o.put(ROOT_CID, headCID);
			_branches.insert(o, WriteConcern.FSYNC_SAFE);
		}
	}
	
	/**
	 * Updates the head of a branch. Always waits for the database to
	 * fsync before returning. This guarantees all threads will see the change.
	 * This operation will usually be the last one when a commit is made, so
	 * fsync'ing here is crucial for the database's integrity. Fsync'ing will
	 * also make the database write all other documents created during the
	 * commit to the hard disk.
	 * @param name the branch's name
	 * @param headCID the CID of the new head
	 */
	public void updateBranchHead(String name, long headCID) {
		_branches.update(new BasicDBObject(MongoDBConstants.ID, name),
				new BasicDBObject("$set", new BasicDBObject(CID, headCID)),
				false, false, WriteConcern.FSYNC_SAFE);
	}
	
	/**
	 * Checks if a branch with the given name exists
	 * @param name the branch's name
	 * @return true if the branch exists, false otherwise
	 */
	public boolean existsBranch(String name) {
		return _branches.count(new BasicDBObject(MongoDBConstants.ID, name)) > 0;
	}
	
	/**
	 * Loads a branch from the database or fails if it does not exist
	 * @param name the branch's name
	 * @return the document representing the branch
	 * @throws VException if the branch does not exist
	 */
	private DBObject findBranch(String name) {
		DBObject branch = _branches.findOne(name);
		if (branch == null) {
			throw new VException("Unknown branch: " + name);
		}
		return branch;
	}
	
	/**
	 * Resolves the head commit of a named branch
	 * @param name the name of the branch to resolve
	 * @return the resolved commit
	 * @throws VException if the commit could not be resolved
	 */
	public Commit resolveBranch(String name) {
		DBObject branch = findBranch(name);
		return resolveCommit((Long)branch.get(CID));
	}

	/**
	 * Resolves the CID of a named branch's root
	 * @param name the branch's name
	 * @return the resolved CID
	 * @throws VException if the branch does not exist
	 */
	public long resolveBranchRootCid(String name) {
		DBObject branch = findBranch(name);
		return (Long)branch.get(ROOT_CID);
	}
	
	/**
	 * Checks if a commit with a given CID exists
	 * @param cid the commit's ID (CID)
	 * @return true if the commit exists, false otherwise
	 */
	public boolean existsCommit(long cid) {
		return _commits.count(new BasicDBObject(MongoDBConstants.ID, cid)) > 0;
	}
	
	/**
	 * Resolves a CID to its corresponding commit
	 * @param cid the CID
	 * @return the commit
	 * @throws VException if the commit is unknown
	 */
	public Commit resolveCommit(long cid) {
		DBObject o = _commits.findOne(cid);
		if (o == null) {
			throw new VException("Unknown commit: " + cid);
		}
		long parentCID = (Long)o.get(PARENT_CID);
		long rootCID = (Long)o.get(ROOT_CID);
		DBObject objs = (DBObject)o.get(OBJECTS);
		Map<String, IdMap> objects = new HashMap<String, IdMap>();
		for (String k : objs.keySet()) {
			if (!k.equals(MongoDBConstants.ID)) {
				objects.put(k, resolveCollectionObjects((DBObject)objs.get(k)));
			}
		}
		return new Commit(cid, parentCID, rootCID, objects);
	}
	
	private IdMap resolveCollectionObjects(DBObject o) {
		Set<String> keys = o.keySet();
		IdMap r = new IdHashMap(keys.size());
		for (String k : keys) {
			r.put(Long.parseLong(k), (Long)o.get(k));
		}
		return r;
	}
	
	@Override
	public long getParent(long cid) {
		DBObject o = _commits.findOne(cid, new BasicDBObject(PARENT_CID, 1));
		if (o == null) {
			throw new VException("Unknown commit: " + cid);
		}
		return (Long)o.get(PARENT_CID);
	}

	@Override
	public long[] getChildren(long cid) {
		if (cid != 0 && !existsCommit(cid)) {
			throw new VException("Unknown commit: " + cid);
		}
		DBCursor c = _commits.find(new BasicDBObject(PARENT_CID, cid),
				new BasicDBObject(MongoDBConstants.ID, 1));
		long[] r = new long[c.count()];
		int i = 0;
		for (DBObject o : c) {
			r[i++] = (Long)o.get(MongoDBConstants.ID);
		}
		return r;
	}
}
