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

package de.fhg.igd.mongomvcc.impl;

import java.io.IOException;

/**
 * A strategy to convert binary data in large objects
 * @author Michel Kraemer
 */
public interface ConvertStrategy {
	/**
	 * Convert the given data to a binary object and return the
	 * replacement OID
	 * @param data the data to convert
	 * @return the OID that should replace the converted data in the
	 * object that holds the data, or 0 (zero) if the data was not converted
	 */
	long convert(Object data);
	
	/**
	 * Load the binary data with the given OID and convert it back to
	 * an object
	 * @param oid the OID
	 * @return the converted object
	 * @throws IOException if the binary data could not be read
	 */
	Object convert(long oid) throws IOException;
}
