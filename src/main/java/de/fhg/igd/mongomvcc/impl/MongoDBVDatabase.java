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

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;

import de.fhg.igd.mongomvcc.VBranch;
import de.fhg.igd.mongomvcc.VConstants;
import de.fhg.igd.mongomvcc.VCounter;
import de.fhg.igd.mongomvcc.VDatabase;
import de.fhg.igd.mongomvcc.VException;
import de.fhg.igd.mongomvcc.VHistory;
import de.fhg.igd.mongomvcc.VMaintenance;
import de.fhg.igd.mongomvcc.helper.IdMap;
import de.fhg.igd.mongomvcc.impl.internal.BuildInfo;
import de.fhg.igd.mongomvcc.impl.internal.Commit;
import de.fhg.igd.mongomvcc.impl.internal.Tree;

/**
 * MongoDB implementation of a Multiversion Concurrency Control database.
 * @author Michel Kraemer
 */
public class MongoDBVDatabase implements VDatabase {
	/**
	 * The MongoDB database object
	 */
	private DB _db;
	
	/**
	 * Provides a thread-safe way to generate new unique IDs
	 */
	private VCounter _counter;
	
	/**
	 * The tree of commits
	 */
	private Tree _tree;
	
	/**
	 * Build information about the database instance (may be null if the
	 * information is not available)
	 */
	private BuildInfo _buildInfo;
	
	@Override
	public void connect(String name) throws VException {
		Mongo mongo;
		try {
			mongo = new Mongo();
		} catch (UnknownHostException e) {
			throw new VException("Unknown host", e);
		}
		connectInternal(name, mongo);
	}
	
	@Override
	public void connect(String name, int port) throws VException {
		connect(name, ServerAddress.defaultHost(), port);
	}
	
	@Override
	public void connect(String name, String host, int port) throws VException {
		Mongo mongo;
		try {
			mongo = new Mongo(new ServerAddress(host, port));
		} catch (UnknownHostException e) {
			throw new VException("Unknown host", e);
		}
		connectInternal(name, mongo);
	}
	
	/**
	 * <p>Connect to a replica set. This method does not appear in the
	 * {@link VDatabase} interface because replica sets are specific to
	 * MongoDB. Other MVCC implementations might not have replica sets.</p>
	 * <p>Besides we can use {@link ServerAddress} and {@link ReadPreference}
	 * here which is not possible in the generic interface.</p>
	 * @param name the database name
	 * @param seeds a list of replica set members
	 * @param readPreference the read preference for this database (can be
	 * null if the default should be used) 
	 */
	public void connectToReplicaSet(String name, List<ServerAddress> seeds,
			ReadPreference readPreference) {
		Mongo mongo = new Mongo(seeds);
		if (readPreference != null) {
			mongo.setReadPreference(readPreference);
		}
		connectInternal(name, mongo);
	}


	private void connectInternal(String name, Mongo mongo) {
		_buildInfo = initBuildInfo(mongo);
		
		_db = mongo.getDB(name);
		_counter = new MongoDBVCounter(_db);
		_tree = new Tree(_db);
		
		//create root commit and master branch if needed
		if (_tree.isEmpty()) {
			Commit root = new Commit(_counter.getNextId(), 0, 0,
					Collections.<String, IdMap>emptyMap());
			_tree.addCommit(root);
			_tree.addBranch(VConstants.MASTER, root.getCID());
		}
	}
	
	/**
	 * Obtains build information from the database instance. If any value is
	 * not available, this method will return <code>null</code>.
	 * @param mongo the database
	 * @return the build information or <code>null</code>
	 */
	private static BuildInfo initBuildInfo(Mongo mongo) {
		DB db = mongo.getDB("admin");
		if (db == null) {
			return null;
		}
		CommandResult cr = db.command("buildInfo");
		String version = (String)cr.get("version");
		if (version == null) {
			return null;
		}
		String[] vss = version.split("\\.");
		if (vss.length <= 2) {
			return null;
		}
		Integer maxBsonObjectSize = (Integer)cr.get("maxBsonObjectSize");
		if (maxBsonObjectSize == null) {
			maxBsonObjectSize = Integer.valueOf(0);
		}
		try {
			return new BuildInfo(Integer.parseInt(vss[0]), Integer.parseInt(vss[1]),
					Integer.parseInt(vss[2]), maxBsonObjectSize);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	/**
	 * @return the underlying MongoDB database
	 */
	public DB getDB() {
		return _db;
	}
	
	/**
	 * @return build information about the database instance (may be null if the
	 * information is not available)
	 */
	public BuildInfo getBuildInfo() {
		return _buildInfo;
	}

	@Override
	public void drop() {
		_db.dropDatabase();
	}
	
	@Override
	public VBranch checkout(String name) {
		long rootCid = _tree.resolveBranchRootCid(name);
		return new MongoDBVBranch(name, rootCid, _tree, this);
	}
	
	/**
	 * Determines the root CID for a new branch. Checks if the commit with
	 * the given CID already belongs to a named branch or if it already has
	 * children. If so, the root for the new branch will be the given CID.
	 * Otherwise it is assumed that the commit is the head of an unnamed
	 * branch and so the commit's root ID will be returned.
	 * @param cid the CID of the commit to branch from
	 * @return the root CID for the new branch
	 */
	private long determineRootForBranch(long cid) {
		Commit head = _tree.resolveCommit(cid);
		long root = head.getRootCID();
		if (_tree.existsBranch(root) || _tree.hasChildren(cid)) {
			//we're trying to create a branch from a commit that belongs
			//to an existing branch. create a new branch from here on.
			root = cid;
		}
		return root;
	}
	
	@Override
	public VBranch checkout(long cid) {
		long root = determineRootForBranch(cid);
		return new MongoDBVBranch(null, root, _tree, this);
	}
	
	@Override
	public VBranch createBranch(String name, long headCID) {
		long root = determineRootForBranch(headCID);
		_tree.addBranch(name, root);
		return new MongoDBVBranch(name, root, _tree, this);
	}
	
	@Override
	public VCounter getCounter() {
		return _counter;
	}
	
	@Override
	public VHistory getHistory() {
		return _tree;
	}
	
	@Override
	public VMaintenance getMaintenance() {
		return new MongoDBVMaintenance(this);
	}
}
