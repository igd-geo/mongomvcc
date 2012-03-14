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
 * <p>A {@link VCollection} which can also handle large objects (BLOBs).</p>
 * <p>This class analyzes inserted objects and handles primitive byte
 * arrays and {@link java.io.InputStream}s correctly. Specialized
 * implementations may also handle other types of large data.</p>
 * <p>If large objects are retrieved later with one of the <code>find</code>
 * methods, this class restores the primitive byte arrays or
 * {@link java.io.InputStream}s.</p>
 * @author Michel Kraemer
 */
public interface VLargeCollection extends VCollection {
	//nothing to add yet
}
