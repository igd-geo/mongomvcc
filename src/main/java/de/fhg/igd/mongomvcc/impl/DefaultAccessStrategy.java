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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.fhg.igd.mongomvcc.VException;

/**
 * The default access strategy iterates through all elements in the
 * given document and replaces binary data by replacement OIDs
 * @author Michel Kraemer
 */
public class DefaultAccessStrategy implements AccessStrategy {
	/**
	 * The attributes that denotes which other attributes point to GridFS files
	 */
	private static final String BINARY_ATTRIBUTES = "_binary_attrs";
	
	/**
	 * The strategy used to convert binary objects
	 */
	private ConvertStrategy _convert;

	@Override
	public void setConvertStrategy(ConvertStrategy cs) {
		_convert = cs;
	}
	
	@Override
	public void onInsert(Map<String, Object> obj) {
		List<String> binaryAttributes = new ArrayList<String>();
		
		//check each attribute for binary data
		for (Map.Entry<String, Object> e : obj.entrySet()) {
			Object v = e.getValue();
			long oid = _convert.convert(v);
			if (oid != 0) {
				e.setValue(oid);
				binaryAttributes.add(e.getKey());
			}
		}
		
		//save which attributes point to GridFS files
		if (!binaryAttributes.isEmpty()) {
			obj.put(BINARY_ATTRIBUTES, binaryAttributes);
		}
	}
	
	@Override
	public void onResolve(Map<String, Object> obj) {
		@SuppressWarnings("unchecked")
		List<String> binaryAttributes = (List<String>)obj.get(BINARY_ATTRIBUTES);
		if (binaryAttributes == null) {
			//nothing to do
			return;
		}
		
		try {
			for (String attr : binaryAttributes) {
				long gridId = (Long)obj.get(attr);
				obj.put(attr, _convert.convert(gridId));
			}
		} catch (IOException e) {
			throw new VException("Could not read binary data", e);
		}
		
		obj.remove(BINARY_ATTRIBUTES);
	}
}
