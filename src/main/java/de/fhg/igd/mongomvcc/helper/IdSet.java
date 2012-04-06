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
 * A set of long values
 * @author Michel Kraemer
 */
public interface IdSet {
	/**
	 * Inserts a value into the set. This operation is essentially
	 * a no-op if the value is already in the set.
	 * @param value the value to insert
	 * @return true if the value has been added, false if it already
	 * was in the set
	 */
	boolean add(long value);
	
	/**
	 * Checks if the set contains a given value
	 * @param value the value
	 * @return true if the set contains the value, false otherwise
	 */
	boolean contains(long value);
	
	/**
	 * Removes a value from the set
	 * @param value the value to remove
	 * @return true if the value was in the set, false otherwise
	 */
	boolean remove(long value);
	
	/**
	 * @return an array containing all values currently in the set
	 */
	long[] toArray();
	
	/**
	 * @return an iterator for this set
	 */
	IdIterator iterator();
	
	/**
	 * @return the number of elements in this set
	 */
	int size();
	
	/**
	 * Removes all elements from this set
	 */
	void clear();
}
