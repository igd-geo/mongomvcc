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
 * <p>Represents a database using Multiversion Concurrency Control (MVCC). A MVCC
 * database consists of a tree which can be split up into several branches.
 * Each branch consists of several commits which contain references to
 * database objects. The MVCC database provides quick access to single objects.
 * How this is achieved, is implementation-specific.</p>
 * <p>The following abbreviations are used throughout the framework:
 * <dl>
 * <dt>CID</dt><dd>Commit ID (unique for each commit in the tree)</dd>
 * <dt>UID</dt><dd>The application-specific unique key for each database
 * object. This ID is independent from the various versions of the object, it
 * exists throughout the whole lifetime of an object, regardless which
 * representation it currently has. Objects added to the database will
 * automatically be assigned a new ID.</dd>
 * </dl>
 * <p><strong>Thread-safety:</strong> this class is thread-safe.</p>
 * @author Michel Kraemer
 */
public interface VDatabase {
	/**
	 * Connect to a database
	 * @param name the database name
	 * @throws VException if connection failed
	 */
	void connect(String name);
	
	/**
	 * Checks out a named branch from the database
	 * @param name the branch's name
	 * @return the branch
	 * @throws VException if the branch does not exist yet
	 */
	VBranch checkout(String name);
	
	/**
	 * Checks out an unnamed branch from the database
	 * @param cid the CID of the commit which should be the branch's root
	 * @return the branch
	 * @throws VException if there is not commit with the given CID
	 */
	VBranch checkout(long cid);
	
	/**
	 * Creates a new named branch whose head is set to the given CID
	 * @param name the branch's name
	 * @param head the branch's head CID
	 * @return the branch
	 * @throws VException if there already is a branch with the given name or
	 * if the given head CID could not be resolved to an existing commit
	 */
	VBranch createBranch(String name, long headCID);
	
	/**
	 * Deletes the whole database. Be very careful with this method!
	 */
	void drop();
	
	/**
	 * @return a counter which generates unique IDs for the database
	 */
	VCounter getCounter();
}
