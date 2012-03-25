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

import java.util.Map;

import gnu.trove.map.hash.TLongLongHashMap;

/**
 * A commit has a CID and stores references to added/changed database objects
 * @author Michel Kraemer
 */
public class Commit {
	/**
	 * The commit's ID
	 */
	private final long _cid;
	
	/**
	 * The CID of this commit's parent. Can be 0 (zero) if there is no
	 * parent (can only happen for the root commit)
	 */
	private final long _parentCID;
	
	/**
	 * The root CID of the branch this commit belongs to
	 */
	private final long _rootCID;
	
	/**
	 * Objects added/changed in this commit. Maps collection names to maps of
	 * UIDs and OIDs.
	 */
	private final Map<String, TLongLongHashMap> _objects;
	
	/**
	 * Constructs a new commit
	 * @param cid the commit's ID
	 * @param parentCID the CID of this commit's parent. Can be 0 (zero) if
	 * there is no parent (can only happen for the root commit)
	 * @param rootCID the root CID of the branch this commit belongs to
	 * @param objects objects added/changed in this commit. Maps collection
	 * names to maps of UIDs and OIDs.
	 */
	public Commit(long cid, long parentCID, long rootCID, Map<String, TLongLongHashMap> objects) {
		_cid = cid;
		_parentCID = parentCID;
		_rootCID = rootCID;
		_objects = objects;
	}
	
	/**
	 * @return the commits's ID
	 */
	public long getCID() {
		return _cid;
	}
	
	/**
	 * @return the CID of this commit's parent. Can be 0 (zero) if there is no
	 * parent (can only happen for the root commit)
	 */
	public long getParentCID() {
		return _parentCID;
	}
	
	/**
	 * @return the root CID of the branch this commit belongs to
	 */
	public long getRootCID() {
		return _rootCID;
	}
	
	/**
	 * @return objects added/changed in this commit. Maps collection
	 * names to maps of UIDs and OIDs. For performance reasons the internal
	 * map is returned here. Callers MUST NEVER change this map.
	 */
	public Map<String, TLongLongHashMap> getObjects() {
		return _objects;
	}
}
