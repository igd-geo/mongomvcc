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

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import de.fhg.igd.mongomvcc.VCursor;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

/**
 * Implementation of {@link VCursor} for MongoDB
 * @author Michel Kraemer
 */
public class MongoDBVCursor implements VCursor {
	private final DBCursor _delegate;
	private final Predicate<DBObject> _filter;
	
	/**
	 * An empty cursor
	 */
	public static VCursor EMPTY = new VCursor() {
		@Override
		public Iterator<Map<String, Object>> iterator() {
			return Iterators.emptyIterator();
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
	public MongoDBVCursor(DBCursor delegate, Predicate<DBObject> filter) {
		_delegate = delegate;
		_filter = filter;
	}
	
	@Override
	public Iterator<Map<String, Object>> iterator() {
		Iterator<DBObject> it = _delegate.iterator();
		if (_filter != null) {
			it = Iterators.filter(it, _filter);
		}
		return Iterators.transform(it, new Function<DBObject, Map<String, Object>>() {
			@SuppressWarnings("unchecked")
			@Override
			public Map<String, Object> apply(DBObject input) {
				if (input instanceof Map) {
					return (Map<String, Object>)input;
				}
				return input.toMap();
			}
		});
	}

	@Override
	public int size() {
		if (_filter != null) {
			//very slow... bummer...
			return Iterators.size(Iterators.filter(_delegate.iterator(), _filter));
		}
		return _delegate.size();
	}
}
