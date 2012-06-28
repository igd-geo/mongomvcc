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
 * Saves build information about the MongoDB databse instance
 * @author Michel Kraemer
 */
public class BuildInfo {
	private final int _majorVersion;
	private final int _minorVersion;
	private final int _revision;
	private final int _maxBsonObjectSize;
	
	/**
	 * Constructs a new object
	 * @param majorVersion the database's major version
	 * @param minorVersion the database's minor version
	 * @param revision the database's revision
	 * @param maxBsonObjectSize the maximum size of BSON documents (can be 0 if unknown)
	 */
	public BuildInfo(int majorVersion, int minorVersion, int revision, int maxBsonObjectSize) {
		_majorVersion = majorVersion;
		_minorVersion = minorVersion;
		_revision = revision;
		_maxBsonObjectSize = maxBsonObjectSize;
	}
	
	/**
	 * @return the database's major version
	 */
	public int getMajorVersion() {
		return _majorVersion;
	}
	
	/**
	 * @return the database's minor version
	 */
	public int getMinorVersion() {
		return _minorVersion;
	}
	
	/**
	 * @return the database's revision
	 */
	public int getRevision() {
		return _revision;
	}
	
	/**
	 * @return the maximum size of BSON documents (can be 0 if unknown)
	 */
	public int getMaxBsonObjectSize() {
		return _maxBsonObjectSize;
	}
}
