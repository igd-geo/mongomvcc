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

import gnu.trove.map.hash.TLongLongHashMap;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.DB;
import com.mongodb.gridfs.GridFS;

import de.fhg.igd.mongomvcc.VBranch;
import de.fhg.igd.mongomvcc.VCollection;
import de.fhg.igd.mongomvcc.VCounter;
import de.fhg.igd.mongomvcc.VException;
import de.fhg.igd.mongomvcc.VLargeCollection;
import de.fhg.igd.mongomvcc.impl.internal.Commit;
import de.fhg.igd.mongomvcc.impl.internal.Index;
import de.fhg.igd.mongomvcc.impl.internal.Tree;

/**
 * <p>Implementation of {@link VBranch} for MongoDB. Each
 * thread owns an index and a head of the branch/commit it has checked out.
 * This ensures isolation. If two threads try to create a commit with the
 * same head, the {@link #commit()} method will fail. The rule is: come first,
 * serve first.</p>
 * TODO In the future, if two commits have the same parent a new branch
 * should be created.
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
	 * The branch's name
	 */
	private final String _name;
	
	/**
	 * The tree of commits
	 */
	private final Tree _tree;
	
	/**
	 * The MongoDB database object
	 */
	private DB _db;
	
	/**
	 * A counter to generate unique IDs
	 */
	private final VCounter _counter;
	
	/**
	 * Constructs a new branch object (not the branch itself)
	 * @param name the branch's name
	 * @param tree the tree of commits
	 * @param db the MongoDB database object
	 * @param counter a counter to generate unique IDs
	 */
	public MongoDBVBranch(String name, Tree tree, DB db, VCounter counter) {
		_name = name;
		_tree = tree;
		_db = db;
		_counter = counter;
	}
	
	/**
	 * @return the thread-local head of this branch
	 */
	private Commit getHead() {
		Commit r = _head.get();
		if (r == null) {
			r = _tree.resolve(_name);
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
			r = new Index(getHead(), _tree);
			_index.set(r);
		}
		return r;
	}
	
	@Override
	public long commit() {
		Index idx = getIndex();
		//clone dirty objects because we clear them below
		Map<String, TLongLongHashMap> dos = new HashMap<String, TLongLongHashMap>(idx.getDirtyObjects());
		Commit head = getHead();
		Commit c = new Commit(_counter.getNextId(), head.getCID(), dos);
		_tree.addCommit(c);
		updateHead(c);
		
		//synchronize the following part, because we first resolve the branch
		//and then update it
		synchronized (this) {
			//check for conflicts (i.e. if another thread has already updated the branch's head)
			if (_tree.resolve(_name).getCID() != c.getParentCID()) {
				throw new VException("Branch " + _name + " has already been " +
						"updated by another commit");
			}
			_tree.updateBranchHead(_name, c.getCID());
		}

		//reset index
		idx.clearDirtyObjects();
		
		return c.getCID();
	}
	
	@Override
	public void rollback() {
		//simply reset the whole index
		_index.remove();
	}
}
