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

import de.fhg.igd.mongomvcc.impl.MongoDBVDatabase;

/**
 * Helps to keep the library compatible to multiple MongoDB versions
 * @author Michel Kraemer
 */
public abstract class CompatibilityHelper {
	private CompatibilityHelper() {
		//nothing to do here
	}
	
	/**
	 * Checks if the database supports the <code>$and</code> operation.
	 * @param db the database
	 * @return true if the database supports <code>$and</code>, false if it
	 * doesn't or if that information is not available
	 */
	public static boolean supportsAnd(MongoDBVDatabase db) {
		BuildInfo bi = db.getBuildInfo();
		return (bi != null && bi.getMajorVersion() >= 2);
	}
}
