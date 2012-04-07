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

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import de.fhg.igd.mongomvcc.VCursor;
import de.fhg.igd.mongomvcc.helper.Filter;
import de.fhg.igd.mongomvcc.helper.FilteringIterator;
import de.fhg.igd.mongomvcc.helper.TransformingIterator;

/**
 * Implementation of {@link VCursor} for MongoDB
 * @author Michel Kraemer
 */
public class MongoDBVCursor implements VCursor {
	private final DBCursor _delegate;
	private final Filter<DBObject> _filter;
	
	/**
	 * An empty cursor
	 */
	public static VCursor EMPTY = new VCursor() {
		@Override
		public Iterator<Map<String, Object>> iterator() {
			return new Iterator<Map<String, Object>>() {
				@Override
				public boolean hasNext() {
					return false;
				}

				@Override
				public Map<String, Object> next() {
					throw new NoSuchElementException();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
				
			};
		}

		@Override
		public int size() {
			return 0;
		}
	};
	
	/**
	 * Constructs a new cursor (without a filter)
	 * @param delegate the actual MongoDB cursor
	 */
	public MongoDBVCursor(DBCursor delegate) {
		this(delegate, null);
	}
	
	/**
	 * Constructs a new cursor
	 * @param delegate the actual MongoDB cursor
	 * @param filter a filter which decides if a DBObject should be included
	 * into the cursor's result or not (can be null)
	 */
	public MongoDBVCursor(DBCursor delegate, Filter<DBObject> filter) {
		_delegate = delegate;
		_filter = filter;
	}
	
	@Override
	public Iterator<Map<String, Object>> iterator() {
		Iterator<DBObject> it = _delegate.iterator();
		if (_filter != null) {
			it = new FilteringIterator<DBObject>(it, _filter);
		}
		return new TransformingIterator<DBObject, Map<String, Object>>(it) {
			@SuppressWarnings("unchecked")
			@Override
			protected Map<String, Object> transform(DBObject input) {
				if (input instanceof Map) {
					return (Map<String, Object>)input;
				}
				return input.toMap();
			}
		};
	}

	@Override
	public int size() {
		if (_filter != null) {
			//very slow... bummer...
			Iterator<DBObject> i = new FilteringIterator<DBObject>(_delegate.iterator(), _filter);
			int n = 0;
			while (i.hasNext()) {
				i.next();
				++n;
			}
			return n;
		}
		return _delegate.size();
	}
}
