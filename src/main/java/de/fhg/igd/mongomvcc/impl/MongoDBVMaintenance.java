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

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import de.fhg.igd.mongomvcc.VHistory;
import de.fhg.igd.mongomvcc.VMaintenance;
import de.fhg.igd.mongomvcc.helper.IdHashSet;
import de.fhg.igd.mongomvcc.helper.IdSet;
import de.fhg.igd.mongomvcc.impl.internal.MongoDBConstants;

/**
 * MongoDB implementation of MVCC database maintenance operations. Locks
 * the database during these operations. This might not be enough if some
 * concurrent thread/process is just in the progress of creating a new commit,
 * but it's better than nothing.
 * @author Michel Kraemer
 */
public class MongoDBVMaintenance implements VMaintenance {
	/**
	 * The database
	 */
	private final MongoDBVDatabase _db;
	
	/**
	 * Constructs the maintenance object
	 * @param db the database
	 */
	public MongoDBVMaintenance(MongoDBVDatabase db) {
		_db = db;
	}
	
	@Override
	public long[] findDanglingCommits(long expiry, TimeUnit unit) {
		_db.getDB().getMongo().fsyncAndLock();
		try {
			return doFindDanglingCommits(expiry, unit);
		} finally {
			_db.getDB().getMongo().unlock();
		}
	}
	
	@Override
	public long pruneDanglingCommits(long expiry, TimeUnit unit) {
		long[] cids = findDanglingCommits(expiry, unit);
		DBCollection collCommits = _db.getDB().getCollection(MongoDBConstants.COLLECTION_COMMITS);
		
		//delete commits in chunks, so we avoid sending an array that is
		//larger than the maximum document size
		final int sliceCount = 1000;
		for (int i = 0; i < cids.length; i += sliceCount) {
			int maxSliceCount = Math.min(sliceCount, cids.length - i);
			long[] slice = new long[maxSliceCount];
			System.arraycopy(cids, i, slice, 0, maxSliceCount);
			collCommits.remove(new BasicDBObject(MongoDBConstants.ID,
					new BasicDBObject("$in", slice)));
		}
		
		return cids.length;
	}
	
	private long[] doFindDanglingCommits(long expiry, TimeUnit unit) {
		long expiryMillis = unit.toMillis(expiry);
		long currentTime = Calendar.getInstance().getTimeInMillis();
		long maxTime = currentTime - expiryMillis;
		
		//load all commits which are older than the expiry time. mark them as dangling
		DBCollection collCommits = _db.getDB().getCollection(MongoDBConstants.COLLECTION_COMMITS);
		DBCursor commits = collCommits.find(new BasicDBObject(MongoDBConstants.TIMESTAMP,
				new BasicDBObject("$not", new BasicDBObject("$gte", maxTime))), //also include commits without a timestamp
				new BasicDBObject(MongoDBConstants.ID, 1));
		IdSet danglingCommits = new IdHashSet();
		for (DBObject o : commits) {
			long cid = (Long)o.get(MongoDBConstants.ID);
			danglingCommits.add(cid);
		}
		
		//walk through all branches and eliminate commits which are not dangling
		DBCollection collBranches = _db.getDB().getCollection(MongoDBConstants.COLLECTION_BRANCHES);
		DBCursor branches = collBranches.find(new BasicDBObject(), new BasicDBObject(MongoDBConstants.CID, 1));
		VHistory history = _db.getHistory();
		IdSet alreadyCheckedCommits = new IdHashSet();
		for (DBObject o : branches) {
			long cid = (Long)o.get(MongoDBConstants.CID);
			while (cid != 0) {
				if (alreadyCheckedCommits.contains(cid)) {
					break;
				}
				alreadyCheckedCommits.add(cid);
				danglingCommits.remove(cid);
				cid = history.getParent(cid);
			}
		}
		
		//all remaining commits must be dangling
		return danglingCommits.toArray();
	}
}
