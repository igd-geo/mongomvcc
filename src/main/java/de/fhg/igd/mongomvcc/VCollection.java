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

package de.fhg.igd.mongomvcc;

import java.util.Map;

/**
 * <p>A collection within a Multiversion Concurrency Control database.</p>
 * <p>Implementations of this interface do not handle large objects (BLOBs).
 * Inserting a large object into this collection may lead to an error if
 * the underlying implementation has a maximum size for documents. You
 * can use a {@link VLargeCollection} for such cases. Nonetheless,
 * implementations of this interface may be significantly faster than thos
 * of {@link VLargeCollection}. Hence, if the objects are definitely not
 * too large, this interface should be used.</p>
 * <p><strong>Thread-safety:</strong> this class is thread-safe.</p>
 * @author Michel Kraemer
 */
public interface VCollection {
	/**
	 * @return the collection's name
	 */
	String getName();
	
	/**
	 * Inserts a new object to the collection. If the object does not have
	 * a UID yet, a new one will be generated and saved in the object's
	 * <code>uid</code> attribute.
	 * @param obj the object to add to the collection
	 */
	void insert(Map<String, Object> obj);
	
	/**
	 * Deletes the object with the given UID from the collection (if it exists)
	 * @param uid the UID of the object to delete
	 */
	void delete(long uid);
	
	/**
	 * Deletes all objects from the collection that match the given example object
	 * @param example the example object
	 */
	void delete(Map<String, Object> example);
	
	/**
	 * @return a cursor which iterates over all objects in this collection
	 */
	VCursor find();
	
	/**
	 * Find by example. Returns all objects that match the given example.
	 * @param example the example object
	 * @return a cursor iterating over all matching objects
	 */
	VCursor find(Map<String, Object> example);
	
	/**
	 * Find by example. Returns all objects that match the given example, but
	 * only return the requested fields. Omit all other fields, thus return
	 * partial objects only.
	 * @param example the example object
	 * @param fields the names of the fields to return
	 * @return a cursor iterating over all matching objects
	 */
	VCursor find(Map<String, Object> example, String... fields);
	
	/**
	 * Finds an object that matches the given example
	 * @param example the example object
	 * @return the object or null if there is no such object
	 */
	Map<String, Object> findOne(Map<String, Object> example);
}
