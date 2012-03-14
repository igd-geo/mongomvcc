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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An abstract implementation of {@link VFactory} which provides
 * some default method implementations
 * @author Michel Kraemer
 */
public abstract class AbstractVFactory implements VFactory {
	@Override
	public Map<String, Object> createDocument() {
		return new HashMap<String, Object>();
	}

	@Override
	public Map<String, Object> createDocument(String key, Object value) {
		Map<String, Object> r = createDocument();
		r.put(key, value);
		return r;
	}

	@Override
	public List<Object> createList() {
		return new ArrayList<Object>();
	}
}
