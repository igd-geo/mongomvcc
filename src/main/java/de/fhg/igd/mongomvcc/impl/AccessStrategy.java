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

import java.util.Map;

/**
 * A strategy used to access large objects
 * @author Michel Kraemer
 */
public interface AccessStrategy {
	/**
	 * Sets the convert strategy that should be used to convert binary data
	 * @param cs the convert strategy
	 */
	void setConvertStrategy(ConvertStrategy cs);
	
	/**
	 * This method will be called for each object that is about to
	 * be put into the collection
	 * @param obj the object
	 */
	void onInsert(Map<String, Object> obj);
	
	/**
	 * This method woll be called for each object that is retrieved
	 * from this collection
	 * @param obj the object
	 */
	void onResolve(Map<String, Object> obj);
}
