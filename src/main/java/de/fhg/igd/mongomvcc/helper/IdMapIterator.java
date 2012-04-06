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

import java.util.NoSuchElementException;

/**
 * Can be used to iterate through {@link IdMap}s
 * @author Michel Kraemer
 */
public interface IdMapIterator {
	/**
	 * @return true if there is a next item in the set
	 */
	boolean hasNext();
	
	/**
	 * Advances to the next item
	 * @throws NoSuchElementException if there is no more item
	 */
	void advance() throws NoSuchElementException;
	
	/**
	 * @return the current key
	 * @throws NoSuchElementException if there is no more item
	 */
	long key() throws NoSuchElementException;
	
	/**
	 * @return the current value
	 * @throws NoSuchElementException if there is no more item
	 */
	long value() throws NoSuchElementException;
}
