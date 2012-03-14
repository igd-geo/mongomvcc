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
 * An unchecked exception that can be thrown by all classes
 * belonging to the MVCC framework
 * @author Michel Kraemer
 */
public class VException extends RuntimeException {
	private static final long serialVersionUID = 4131582095330835424L;

	/**
	 * @see RuntimeException#RuntimeException()
	 */
	public VException() {
		super();
	}
	
	/**
	 * @see RuntimeException#RuntimeException(String)
	 */
	public VException(String message) {
		super(message);
	}
	
	/**
	 * @see RuntimeException#RuntimeException(Throwable)
	 */
	public VException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * @see RuntimeException#RuntimeException(String, Throwable)
	 */
	public VException(String message, Throwable cause) {
		super(message, cause);
	}
}
