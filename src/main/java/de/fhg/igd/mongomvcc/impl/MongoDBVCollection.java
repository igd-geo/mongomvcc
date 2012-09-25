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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import de.fhg.igd.mongomvcc.VCollection;
import de.fhg.igd.mongomvcc.VConstants;
import de.fhg.igd.mongomvcc.VCounter;
import de.fhg.igd.mongomvcc.VCursor;
import de.fhg.igd.mongomvcc.helper.Filter;
import de.fhg.igd.mongomvcc.helper.IdMap;
import de.fhg.igd.mongomvcc.impl.internal.Index;
import de.fhg.igd.mongomvcc.impl.internal.MongoDBConstants;

/**
 * Implements {@link VCollection} for MongoDB
 * @author Michel Kraemer
 */
public class MongoDBVCollection implements VCollection {
	/**
	 * The attribute for the unique ID
	 */
	protected final static String UID = VConstants.UID;
	
	/**
	 * The ID unique for each new object, regardless if it is really new
	 * or a new copy of an already existing object
	 */
	protected final static String OID = MongoDBConstants.ID;
	
	/**
	 * The actual MongoDB collection
	 */
	private final DBCollection _delegate;
	
	/**
	 * The collection's name
	 */
	private final String _name;
	
	/**
	 * The branch currently checked out
	 */
	private final MongoDBVBranch _branch;
	
	/**
	 * A counter to generate unique IDs
	 */
	private final VCounter _counter;
	
	/**
	 * A predicate which filters out objects with OIDs not in the current index
	 */
	private final class OIDInIndexFilter implements Filter<DBObject> {
		private final Index _idx;
		
		OIDInIndexFilter() {
			_idx = _branch.getIndex();
		}
		
		@Override
		public boolean filter(DBObject input) {
			return _idx.containsOID(_name, (Long)input.get(OID));
		}
	}

	/**
	 * Creates a new MongoDBVCollection.
	 * @param delegate the actual MongoDB collection
	 * @param branch the branch currently checked out
	 * @param counter a counter to generate unique IDs
	 */
	public MongoDBVCollection(DBCollection delegate, MongoDBVBranch branch,
			VCounter counter) {
		_delegate = delegate;
		_name = delegate.getName();
		_branch = branch;
		_counter = counter;
	}

	@Override
	public void insert(Map<String, Object> obj) {
		//check UID (throws ClassCastException of UID is no Long, this
		//is by intention)
		Long uid = (Long)obj.get(UID);
		if (uid == null) {
			uid = _counter.getNextId();
			obj.put(UID, uid);
		}
		
		//generate OID
		long oid = _counter.getNextId();
		obj.put(OID, oid);
		
		DBObject dbo;
		if (obj instanceof DBObject) {
			dbo = (DBObject)obj;
		} else {
			dbo = new BasicDBObject(obj);
		}
		
		//insert timestamp
		dbo.put(MongoDBConstants.TIMESTAMP, System.currentTimeMillis());
		
		//insert object into database
		_delegate.insert(dbo);
		
		if (dbo == obj) {
			//remove timestamp from original document
			obj.remove(MongoDBConstants.TIMESTAMP);
		}
		
		//insert object into index
		_branch.getIndex().insert(_name, uid, oid);
	}
	
	@Override
	public void delete(long uid) {
		_branch.getIndex().delete(_name, uid);
	}
	
	@Override
	public void delete(Map<String, Object> example) {
		VCursor allIds = find(example, UID);
		for (Map<String, Object> id : allIds) {
			_branch.getIndex().delete(_name, (Long)id.get(UID));
		}
	}
	
	/**
	 * Creates a new cursor. Subclasses may override this method to
	 * add specialized filters and converters.
	 * @param delegate the actual MongoDB cursor
	 * @param filter a filter which decides if a DBObject should be included
	 * into the cursor's result or not (can be null)
	 * @return the cursor
	 */
	protected VCursor createCursor(DBCursor delegate, Filter<DBObject> filter) {
		return new MongoDBVCursor(delegate, filter);
	}
	
	@Override
	public VCursor find() {
		//ask index for OIDs
		IdMap objs = _branch.getIndex().find(_name);
		if (objs.size() == 0) {
			return MongoDBVCursor.EMPTY;
		}

		//ask MongoDB for objects with the given OIDs
		if (objs.size() == 1) {
			//shortcut for one object
			return createCursor(_delegate.find(new BasicDBObject(OID, objs.values()[0])), null);
		} else {
			DBObject qo = new BasicDBObject();
			qo.putAll(_branch.getQueryObject());
			return createCursor(_delegate.find(qo), new OIDInIndexFilter());
		}
	}
	
	@Override
	public VCursor find(Map<String, Object> example) {
		DBObject o = new BasicDBObject();
		o.putAll(_branch.getQueryObject());
		o.putAll(example);
		return createCursor(_delegate.find(o), new OIDInIndexFilter());
	}
	
	@Override
	public VCursor find(Map<String, Object> example, String... fields) {
		DBObject fo = new BasicDBObject();
		for (String f : fields) {
			fo.put(f, 1);
		}
		
		//make sure UID and OID are also returned
		fo.put(UID, 1);
		fo.put(OID, 1);
		
		//exclude lifetime
		//FIXME MongoDB cannot currently mix including and excluding fields
		//FIXME if this is an issue for you, vote for https://jira.mongodb.org/browse/SERVER-391
		//fo.putAll(EXCLUDELIFETIME);
		
		DBObject o = new BasicDBObject();
		o.putAll(_branch.getQueryObject());
		o.putAll(example);
		return createCursor(_delegate.find(o, fo), new OIDInIndexFilter());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> findOne(Map<String, Object> example) {
		DBObject o = new BasicDBObject();
		o.putAll(_branch.getQueryObject());
		o.putAll(example);
		OIDInIndexFilter filter = new OIDInIndexFilter();
		DBCursor c = _delegate.find(o);
		for (DBObject obj : c) {
			if (filter.filter(obj)) {
				if (obj instanceof Map) {
					return (Map<String, Object>)obj;
				}
				return obj.toMap();
			}
		}
		return null;
	}
	
	@Override
	public String getName() {
		return _name;
	}
	
	/**
	 * @return the counter used to generate unique IDs
	 */
	protected VCounter getCounter() {
		return _counter;
	}
}
