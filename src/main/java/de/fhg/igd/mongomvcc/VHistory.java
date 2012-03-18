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
 * Provides access to detail information about commits (such as attributes,
 * parent commit and children)
 * @author Michel Kraemer
 */
public interface VHistory {
	/**
	 * Retrieves a commit's parent CID
	 * @param cid the commit's CID
	 * @return the CID of the commit's parent (may be 0 if the commit
	 * is the root commit)
	 * @throws VException if there is no commit with such a CID
	 */
	long getParent(long cid);
	
	/**
	 * Retrieves the CIDs of a commit's children
	 * @param cid the commit's CID (or 0 if the CID of the root commit should
	 * be returned)
	 * @return the CIDs of the commit's children (never null, but may be
	 * empty if the commit has no children)
	 * @throws VException if CID != 0 and there is no commit with such a CID
	 */
	long[] getChildren(long cid);
}
