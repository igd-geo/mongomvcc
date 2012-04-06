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

package de.fhg.igd.mongomvcc.helper;

/**
 * Maps from IDs to IDs
 * @author Michel Kraemer
 */
public interface IdMap extends IdCollection {
	/**
	 * Puts a key-value pair into the map. This operation overwrites
	 * the old value if the key is already in the map.
	 * @param key the key to insert
	 * @param value the value to insert
	 * @return the old, overwritten value or 0 if there was no such key
	 */
	long put(long key, long value);
	
	/**
	 * Checks if the map contains a given key
	 * @param key the key
	 * @return true if the map contains the key, false otherwise
	 */
	boolean containsKey(long key);
	
	/**
	 * Removes a key-value pair from the map
	 * @param key the key of the pair to remove
	 * @return the removed value or 0 if the key was not in the map
	 */
	long remove(long key);
	
	/**
	 * Retrieves the value for a given key
	 * @param key the key
	 * @return the value or 0 if the value was not in the map
	 */
	long get(long key);
	
	/**
	 * @return the map's keys as an array
	 */
	long[] keys();
	
	/**
	 * @return the map's values as an array
	 */
	long[] values();
	
	/**
	 * @return an iterator for this map
	 */
	IdMapIterator iterator();
}
