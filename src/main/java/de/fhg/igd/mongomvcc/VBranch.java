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

/**
 * A branch that has been checked out from a {@link VDatabase}.
 * @author Michel Kraemer
 */
public interface VBranch {
	/**
	 * @return the CID of the branch's head
	 */
	long getHead();
	
	/**
	 * <p>Gets or creates a database collection. Collections have no special
	 * meaning, they are just used to group objects.</p>
	 * <p>The collection's state depends on the branch currently checkout
	 * out.</p>
	 * @param name the collection's name
	 * @return the collection (never null)
	 */
	VCollection getCollection(String name);
	
	/**
	 * Gets or creates a database collection that can handle large
	 * objects (BLOBs).
	 * @param name the collection's name
	 * @return the collection (never null)
	 */
	VLargeCollection getLargeCollection(String name);
	
	/**
	 * Commit objects that have been added or changed in this branch
	 * to the database
	 * @return the new commit's ID (CID)
	 */
	long commit();
	
	/**
	 * Resets this branch (i.e. discards all changes made since
	 * the branch has been checked out or since the last commit)
	 */
	void rollback();
}
