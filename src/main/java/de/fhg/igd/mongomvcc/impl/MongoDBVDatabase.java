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

import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.Mongo;

import de.fhg.igd.mongomvcc.VBranch;
import de.fhg.igd.mongomvcc.VConstants;
import de.fhg.igd.mongomvcc.VCounter;
import de.fhg.igd.mongomvcc.VDatabase;
import de.fhg.igd.mongomvcc.VException;
import de.fhg.igd.mongomvcc.VHistory;
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
		try {
			return new BuildInfo(Integer.parseInt(vss[0]), Integer.parseInt(vss[1]),
					Integer.parseInt(vss[2]));
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
	
	@Override
	public VBranch checkout(long cid) {
		if (!_tree.existsCommit(cid)) {
			throw new VException("Unknown commit: " + cid);
		}
		return new MongoDBVBranch(null, cid, _tree, this);
	}
	
	@Override
	public VBranch createBranch(String name, long headCID) {
		Commit head = _tree.resolveCommit(headCID);
		_tree.addBranch(name, headCID);
		return new MongoDBVBranch(name, head.getRootCID(), _tree, this);
	}
	
	@Override
	public VCounter getCounter() {
		return _counter;
	}
	
	@Override
	public VHistory getHistory() {
		return _tree;
	}
}
