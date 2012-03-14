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

import java.util.List;
import java.util.Map;

/**
 * A factory for MVCC related objects. MVCC model implementations may
 * return specialized maps and lists here which can be quickly converted
 * to native database documents. For example, the MongoDB implementation
 * returns BasicDBObjects and BasicDBLists which implement the Map and
 * List interfaces respectively, but can be put directly into the database
 * without any further conversion.
 * @author Michel Kraemer
 */
public interface VFactory {
	/**
	 * @return a new MVCC database
	 */
	VDatabase createDatabase();
	
	/**
	 * @return a new empty document
	 */
	Map<String, Object> createDocument();
	
	/**
	 * Convenience method to create a new document with exactly one element
	 * @param key the element's key
	 * @param value the element's value
	 * @return the new document
	 */
	Map<String, Object> createDocument(String key, Object value);
	
	/**
	 * @return a new list
	 */
	List<Object> createList();
}
