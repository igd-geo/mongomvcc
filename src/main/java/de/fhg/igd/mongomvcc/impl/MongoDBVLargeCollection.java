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

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;

import de.fhg.igd.mongomvcc.VCounter;
import de.fhg.igd.mongomvcc.VCursor;
import de.fhg.igd.mongomvcc.VLargeCollection;

/**
 * Saves primitive byte arrays and {@link InputStream}s in MongoDB's
 * {@link GridFS}.
 * @author Michel Kraemer
 */
public class MongoDBVLargeCollection extends MongoDBVCollection implements
		VLargeCollection {
	/**
	 * A cursor which calls {@link AccessStrategy#onResolve(Map)} for
	 * each object
	 */
	private class MongoDBVLargeCursor extends MongoDBVCursor {
		/**
		 * @see MongoDBVCursor#MongoDBVCursor(DBCursor, Predicate)
		 */
		public MongoDBVLargeCursor(DBCursor delegate, Predicate<DBObject> filter) {
			super(delegate, filter);
		}
		
		@Override
		public Iterator<Map<String, Object>> iterator() {
			return Iterators.transform(super.iterator(), new Function<Map<String, Object>, Map<String, Object>>() {
				@Override
				public Map<String, Object> apply(Map<String, Object> input) {
					_accessStrategy.onResolve(input);
					return input;
				}
			});
		}
	}
	
	/**
	 * The attribute that references a GridFS file's parent object
	 */
	private static final String PARENT = "parent";
	
	/**
	 * The MongoDB GridFS storing binary data
	 */
	private final GridFS  _gridFS;
	
	/**
	 * A strategy used to access large objects
	 */
	private final AccessStrategy _accessStrategy;
	
	/**
	 * Creates a new MongoDBVLargeCollection.
	 * @param delegate the actual MongoDB collection
	 * @param gridFS the MongoDB GridFS storing binary data
	 * @param branch the branch currently checked out
	 * @param counter a counter to generate unique IDs
	 */
	public MongoDBVLargeCollection(DBCollection delegate, GridFS gridFS,
			MongoDBVBranch branch, VCounter counter) {
		this(delegate, gridFS, branch, counter, new DefaultAccessStrategy());
	}
	
	/**
	 * Creates a new MongoDBVLargeCollection with a special access strategy
	 * for large binary objects.
	 * @param delegate the actual MongoDB collection
	 * @param gridFS the MongoDB GridFS storing binary data
	 * @param branch the branch currently checked out
	 * @param counter a counter to generate unique IDs
	 * @param accessStrategy the strategy that should be used to access large objects
	 */
	public MongoDBVLargeCollection(DBCollection delegate, GridFS gridFS,
			MongoDBVBranch branch, VCounter counter, AccessStrategy accessStrategy) {
		super(delegate, branch, counter);
		_gridFS = gridFS;
		_accessStrategy = accessStrategy;
	}

	@Override
	public void insert(Map<String, Object> obj) {
		DefaultConvertStrategy cs = new DefaultConvertStrategy(_gridFS, getCounter());
		_accessStrategy.setConvertStrategy(cs);
		_accessStrategy.onInsert(obj);
		
		//save original object
		super.insert(obj);
		
		//save GridFS files
		for (GridFSInputFile file : cs.getConvertedFiles()) {
			file.put(PARENT, obj.get(OID));
			file.save();
		}
	}
	
	@Override
	protected VCursor createCursor(DBCursor delegate, Predicate<DBObject> filter) {
		return new MongoDBVLargeCursor(delegate, filter);
	}
	
	@Override
	public Map<String, Object> findOne(Map<String, Object> example) {
		Map<String, Object> r = super.findOne(example);
		if (r == null) {
			return null;
		}
		DefaultConvertStrategy cs = new DefaultConvertStrategy(_gridFS, getCounter());
		_accessStrategy.setConvertStrategy(cs);
		_accessStrategy.onResolve(r);
		return r;
	}
}
