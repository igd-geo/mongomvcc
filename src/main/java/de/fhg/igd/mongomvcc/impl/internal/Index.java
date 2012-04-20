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

import de.fhg.igd.mongomvcc.helper.IdHashMap;
import de.fhg.igd.mongomvcc.helper.IdHashSet;
import de.fhg.igd.mongomvcc.helper.IdMap;
import de.fhg.igd.mongomvcc.helper.IdMapIterator;
import de.fhg.igd.mongomvcc.helper.IdSet;
import de.fhg.igd.mongomvcc.impl.MongoDBVDatabase;

/**
 * <p>Provides access to the branch/commit currently checked out.
 * Stores information on dirty objects. Dirty objects are those
 * which have been added or changed before the next commit.</p>
 * <p><strong>Thread-safety:</strong> This class is NOT thread-safe.
 * {@link MongoDBVDatabase} will hold a thread-local variable to restrict
 * access to this object.</p>
 * @author Michel Kraemer
 */
public class Index {
	/**
	 * Maps collection names to maps of UIDs and OIDs
	 */
	private final Map<String, IdMap> _objects = new HashMap<String, IdMap>();
	
	/**
	 * The UIDs of all dirty objects within a collection (a subset of {@link #_objects})
	 */
	private final Map<String, IdMap> _dirtyObjects = new HashMap<String, IdMap>();
	
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
		for (Map.Entry<String, IdMap> e : c.getObjects().entrySet()) {
			IdMap m = getObjects(e.getKey());
			IdSet o = getOIDs(e.getKey());
			IdMapIterator it = e.getValue().iterator();
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
	private IdMap getObjects(String collection) {
		IdMap objs = _objects.get(collection);
		if (objs == null) {
			objs = new IdHashMap();
			_objects.put(collection, objs);
		}
		return objs;
	}

	/**
	 * For a given collection, this method lazily retrieves
	 * the map that maps UIDs of dirty objects to OIDs.
	 * @param collection the collection's name
	 * @return the map
	 */
	private IdMap getDirtyObjects(String collection) {
		IdMap objs = _dirtyObjects.get(collection);
		if (objs == null) {
			objs = new IdHashMap();
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
		IdMap objs = getObjects(collection);
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
	public IdMap find(String collection) {
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
	public Map<String, IdMap> getDirtyObjects() {
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
