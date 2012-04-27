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

/**
 * Constants for the MongoDB database implementation
 * @author Michel Kraemer
 */
public abstract class MongoDBConstants {
	private MongoDBConstants() {
		//Nope, you can't extend this class. No way. Only Chuck Norris can. And
		//I'm sure you're not Chuck Norris, because he doesn't read comments.
	}
	
	/**
	 * The unique ID of each document
	 */
	public static final String ID = "_id";
	
	/**
	 * A commit ID
	 */
	public final static String CID = "cid";
	
	/**
	 * Information about a document's lifetime
	 */
	public static final String LIFETIME = "_lifetime";
	
	/**
	 * The timestamp of a commit or a document
	 */
	public final static String TIMESTAMP = "_time";
	
	/**
	 * The name of the collection containing branches
	 */
	public final static String COLLECTION_BRANCHES = "_branches";
	
	/**
	 * The name of the collection containing commits
	 */
	public final static String COLLECTION_COMMITS = "_commits";
}
