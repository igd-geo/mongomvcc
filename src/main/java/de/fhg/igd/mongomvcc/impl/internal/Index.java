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

import gnu.trove.iterator.TLongLongIterator;
import gnu.trove.map.hash.TLongLongHashMap;

import java.util.HashMap;
import java.util.Map;

import de.fhg.igd.mongomvcc.helper.IdHashSet;
import de.fhg.igd.mongomvcc.helper.IdSet;
import de.fhg.igd.mongomvcc.impl.MongoDBVCollection;
import de.fhg.igd.mongomvcc.impl.MongoDBVDatabase;

/**
 * <p>Provides access to the branch/commit currently checked out.
 * Stores information on dirty objects. Dirty objects are those
 * which have been added or changed before the next commit.</p>
 * <p><strong>Thread-safety:</strong> This class is NOT thread-safe.
 * {@link MongoDBVDatabase} will hold a thread-local variable to restrict
 * access to this object.</p>
 * <p><strong>Hints for optimization:</strong> this implementation currently
 * still provides a lot possibilities to optimize.
 * <ul>
 * <li>One could differentiate between A and B commits. A commits would
 * contain the whole index (without deleted objects) and B commits would only
 * contain changes since their respective parent. Therefore, rebuilding the
 * index could be a lot faster</li>
 * <li>Currently the find methods of {@link MongoDBVCollection} use
 * {@link #containsOID(String, long)} to check if an object exists in this
 * index. Therefore, they load the whole object (only to use its OID).
 * It should be checked, if it would be faster to first retrieve OIDs only
 * and then (after filtering) make a second query.</li>
 * <li>Currently, each thread owns a separate index. In the future a pool
 * of index objects could be created, so several threads could share the same
 * index (for example if they only do read operations). If one thread
 * performs a write operation, the index instance should be cloned before.
 * The index instances in the pool could also be kept in memory, even if
 * there is no thread accessing the database anymore. This would allow
 * other threads in the future to quickly checkout branches/commits, if
 * they have already been indexed.</li>
 * <li>Using this implementation in practise will show if keeping all index
 * instances in memory is a good idea or if it leads to OutOfMemoryErrors.
 * Alternatively, indexes could be swapped out or the index could
 * be kept completely within the database (if that's possible).</li>
 * <li>There is a maximum BSON document size which directly affects the maximum
 * size of commits in the database (and therefore the maximum number of dirty
 * objects in the index). We will have to see how this performs in practise.
 * We may have to introduce chunked commits where each chunk adheres the
 * maximum BSON document size</li>
 * <li>It would be a nice idea to save the number of objects in the index
 * for each commit. This number could be used to pre-allocate the hash maps
 * in the index before the commits are resolved in {@link #readCommit(Commit, Tree)}</li>
 * </ul>
 * </p>
 * @author Michel Kraemer
 */
public class Index {
	/**
	 * Maps collection names to maps of UIDs and OIDs
	 */
	private final Map<String, TLongLongHashMap> _objects = new HashMap<String, TLongLongHashMap>();
	
	/**
	 * The UIDs of all dirty objects within a collection (a subset of {@link #_objects})
	 */
	private final Map<String, TLongLongHashMap> _dirtyObjects = new HashMap<String, TLongLongHashMap>();
	
	/**
	 * The OIDs of all deleted objects within a collection
	 */
	private final Map<String, IdSet> _deletedOids = new HashMap<String, IdSet>();
	
	/**
	 * The OIDs of all objects within a collection (always equals the OIDs in
	 * {@link #_objects} for the same collection)
	 */
	private final Map<String, IdSet> _oids = new HashMap<String, IdSet>();
	
	/**
	 * Construct a new index. Reads the head commit and all its ancestors from
	 * the tree and builds up the index.
	 * @param head the head commit of the branch/commit current checked out
	 * @param tree the tree of commits
	 */
	public Index(Commit head, Tree tree) {
		readCommit(head, tree);
	}
	
	/**
	 * Recursively reads the information from the given commit and all its
	 * ancestors and builds up the index
	 * @param c the commit to read
	 * @param tree the tree of commits
	 */
	private void readCommit(Commit c, Tree tree) {
		if (c.getParentCID() != 0) {
			//recursively read parent commit (if there is any)
			Commit parent = tree.resolveCommit(c.getParentCID());
			readCommit(parent, tree);
		}

		//read objects from the given commit and put them into the index
		for (Map.Entry<String, TLongLongHashMap> e : c.getObjects().entrySet()) {
			TLongLongHashMap m = getObjects(e.getKey());
			IdSet o = getOIDs(e.getKey());
			TLongLongIterator it = e.getValue().iterator();
			while (it.hasNext()) {
				it.advance();
				if (it.value() < 0) {
					//deleted object
					long prev = m.get(it.key());
					if (prev != 0) {
						m.remove(it.key());
						o.remove(prev);
					}
				} else {
					long prev = m.put(it.key(), it.value());
					if (prev != 0) {
						//overwrite object with new value
						o.remove(prev);
					}
					o.add(it.value());
				}
			}
		}
	}

	/**
	 * For a given collection, this method lazily retrieves
	 * the map that maps UIDs to OIDs
	 * @param collection the collection's name
	 * @return the map
	 */
	private TLongLongHashMap getObjects(String collection) {
		TLongLongHashMap objs = _objects.get(collection);
		if (objs == null) {
			objs = new TLongLongHashMap();
			_objects.put(collection, objs);
		}
		return objs;
	}

	/**
	 * For a given collection, this method lazily retrieves
	 * the map that maps UIDs of deleted objects to OIDs.
	 * @param collection the collection's name
	 * @return the map
	 */
	private TLongLongHashMap getDirtyObjects(String collection) {
		TLongLongHashMap objs = _dirtyObjects.get(collection);
		if (objs == null) {
			objs = new TLongLongHashMap();
			_dirtyObjects.put(collection, objs);
		}
		return objs;
	}
	
	/**
	 * For a given collection, this method lazily retrieves
	 * the OIDs of all deleted objects.
	 * @param collection the collection's name
	 * @return the OIDs
	 */
	private IdSet getDeletedOids(String collection) {
		IdSet oids = _deletedOids.get(collection);
		if (oids == null) {
			oids = new IdHashSet();
			_deletedOids.put(collection, oids);
		}
		return oids;
	}

	/**
	 * For a given collection, this method returns the OIDs of all objects
	 * @param collection the collection's name
	 * @return the OIDs
	 */
	private IdSet getOIDs(String collection) {
		IdSet oids = _oids.get(collection);
		if (oids == null) {
			oids = new IdHashSet();
			_oids.put(collection, oids);
		}
		return oids;
	}
	
	/**
	 * Inserts a new object into the index and marks it as dirty
	 * @param collection the name of the collection the object has been added to
	 * @param uid the new object's UID
	 * @param oid the OID
	 */
	public void insert(String collection, long uid, long oid) {
		TLongLongHashMap objs = getObjects(collection);
		IdSet oids = getOIDs(collection);
		long prev = objs.put(uid, oid);
		if (prev != 0) {
			//an existing object is replaced by a new instance. Remove the
			//old OID, since the old object is now invalid (within this index)
			oids.remove(prev);
			
			if (oid == -1) {
				getDeletedOids(collection).add(prev);
			}
		}
		if (oid != -1) {
			oids.add(oid);
		}
		getDirtyObjects(collection).put(uid, oid);
	}
	
	/**
	 * Deletes an object from this index (effectively replaces its OID with
	 * a negative number to mark it as deleted within the commit). This
	 * operation is a NOOP if the object does not exist within the given
	 * collection.
	 * @param collection the name of the collection that contains the object
	 * @param uid the UID of the object to delete
	 */
	public void delete(String collection, long uid) {
		if (getObjects(collection).containsKey(uid)) {
			//overwrite object with a negative OID1. when the
			//index is rebuilt, such objects will be ignored.
			insert(collection, uid, -1);
		}
	}
	
	/**
	 * Return all objects for a given collection. For performance reasons
	 * the internal map is returned. Callers MUST NOT change this map.
	 * @param collection the collection's name
	 * @return the objects
	 */
	public TLongLongHashMap find(String collection) {
		return getObjects(collection);
	}
	
	/**
	 * Checks if the index contains an object with the given OID
	 * @param collection the collection that is supposed to contain the object
	 * @param oid the OID
	 * @return true if the collection contains such an object, false otherwise
	 */
	public boolean containsOID(String collection, long oid) {
		return getOIDs(collection).contains(oid);
	}
	
	/**
	 * @return all dirty objects for all collections. For performance reasons
	 * the internal map is returned. Callers MUST NOT change this map.
	 */
	public Map<String, TLongLongHashMap> getDirtyObjects() {
		return _dirtyObjects;
	}
	
	/**
	 * @return a map that maps collections to the OIDs of objects that
	 * have been deleted in this collection
	 */
	public Map<String, IdSet> getDeletedOids() {
		return _deletedOids;
	}
	
	/**
	 * Clears the map of dirty objects and the map of deleted OIDs
	 */
	public void clearDirtyObjects() {
		_dirtyObjects.clear();
		_deletedOids.clear();
	}
}
