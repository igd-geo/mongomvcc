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
}
