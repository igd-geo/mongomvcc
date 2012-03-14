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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import de.fhg.igd.mongomvcc.VCounter;

/**
 * Implementation of {@link VCounter} for MongoDB. Uses a high-low strategy to
 * obtain a pool of free IDs on each call to {@link #updateNextId()}.
 * @author Carsten Steul
 */
public class MongoDBVCounter implements VCounter {
	/**
     * Collection names
     */
    private final static String COLLECTION_COUNTER = "_counter";
    
	private DBCollection _counter;
	private long _nextId;
	
	/**
	 * Default constructor
	 * @param db the MongoDB database
	 */
	public MongoDBVCounter(DB db) {
		_counter = db.getCollection(COLLECTION_COUNTER);
		_nextId = 0;
		if(_counter.findOne("counter") == null) {
			_counter.insert(new BasicDBObject("_id", "counter").append("c", 0L));
		}
		updateNextId();
	}
	
	private void updateNextId() {
		DBObject doc = _counter.findAndModify(new BasicDBObject("_id", "counter"),
				new BasicDBObject("$inc", new BasicDBObject("c", 0x10000)));
		_nextId = (Long) doc.get("c");
		if (_nextId == 0) {
			++_nextId;
		}
	}
	
	@Override
	public synchronized long getNextId() {
		if((_nextId & 0xFFFF) == 0xFFFF ) {
			updateNextId();
		}
		return _nextId++;
	}
}
