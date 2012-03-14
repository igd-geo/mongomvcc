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

import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import de.fhg.igd.mongomvcc.VDatabase;
import de.fhg.igd.mongomvcc.VFactory;

/**
 * Creates MongoDB implementation-specific objects of the MVCC model
 * @author Michel Kraemer
 */
public class MongoDBVFactory implements VFactory {
	@Override
	public VDatabase createDatabase() {
		return new MongoDBVDatabase();
	}

	@Override
	public Map<String, Object> createDocument() {
		return new BasicDBObject();
	}

	@Override
	public Map<String, Object> createDocument(String key, Object value) {
		return new BasicDBObject(key, value);
	}

	@Override
	public List<Object> createList() {
		return new BasicDBList();
	}
}
