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

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import de.fhg.igd.mongomvcc.VBranch;
import de.fhg.igd.mongomvcc.VCollection;
import de.fhg.igd.mongomvcc.VConstants;
import de.fhg.igd.mongomvcc.VDatabase;
import de.fhg.igd.mongomvcc.VFactory;

/**
 * Compares the performance of the MVCC implementation to plain old MongoDB
 * @author Michel Kraemer
 */
@Ignore
public class MongoDBVDatabaseBenchmark extends AbstractBenchmark {
	protected VDatabase _mvccdb;
	protected VBranch _master;
	protected static final VFactory _factory = new MongoDBVFactory();
	protected DB _db;
	
	private static final int DOCUMENTS = 5000;
	
	/**
	 * Before all unit tests run, make sure the database is clean
	 * @throws Exception if the test could not connect to the database
	 */
	@BeforeClass
	public static void setUpClass() throws Exception {
		Mongo mongo = new Mongo();
		mongo.dropDatabase("mvcctest");
	}
	
	/**
	 * Setup test database
	 * @throws Exception if the test could not connect to the database
	 */
	@Before
	public void setUp() throws Exception {
		_mvccdb = _factory.createDatabase();
		_mvccdb.connect("mvcctest");
		_master = _mvccdb.checkout(VConstants.MASTER);
		
		Mongo mongo = new Mongo();
		_db = mongo.getDB("mvcctest");
	}
	
	/**
	 * Delete test database
	 */
	@After
	public void tearDown() {
		_mvccdb.drop();
	}
	
	private void plainOldInsert(int offset) {
		DBCollection coll = _db.getCollection("persons");
		for (long i = 0; i < DOCUMENTS; ++i) {
			BasicDBObject obj = new BasicDBObject();
			obj.put("name", String.valueOf(i));
			obj.put("age", i + offset);
			obj.put("_id", 1 + i);
			coll.insert(obj);
		}
	}
	
	private void mvccInsert(int offset) {
		VCollection coll = _master.getCollection("persons");
		for (long i = 0; i < DOCUMENTS; ++i) {
			Map<String, Object> obj = _factory.createDocument();
			obj.put("name", String.valueOf(i));
			obj.put("age", i + offset);
			obj.put("uid", 1 + i + offset);
			coll.insert(obj);
		}
	}
	
	/**
	 * Inserts a lot of documents using plain old MongoDB
	 */
	@Test
	@BenchmarkOptions(benchmarkRounds = 2, warmupRounds = 1)
	public void plainOldInsert() {
		plainOldInsert(0);
	}
	
	/**
	 * Inserts a lot of documents using MongoMVCC
	 */
	@Test
	@BenchmarkOptions(benchmarkRounds = 2, warmupRounds = 1)
	public void mvccInsert() {
		mvccInsert(0);
	}
	
	/**
	 * Inserts a lot of documents using plain old MongoDB and then deletes them
	 */
	@Test
	@BenchmarkOptions(benchmarkRounds = 2, warmupRounds = 1)
	public void plainOldInsertDelete() {
		plainOldInsert();
		DBCollection coll = _db.getCollection("persons");
		for (long i = 0; i < DOCUMENTS; ++i) {
			coll.remove(new BasicDBObject("_id", 1 + i));
		}
	}
	
	/**
	 * Inserts a lot of documents using MongoMVCC and then deletes them
	 */
	@Test
	@BenchmarkOptions(benchmarkRounds = 2, warmupRounds = 1)
	public void mvccInsertDelete() {
		mvccInsert();
		VCollection coll = _master.getCollection("persons");
		for (long i = 0; i < DOCUMENTS; ++i) {
			coll.delete(1 + i);
		}
	}
	
	/**
	 * Inserts a lot of documents using plain old MongoDB, then deletes them
	 * and inserts another bunch of documents
	 */
	@Test
	@BenchmarkOptions(benchmarkRounds = 2, warmupRounds = 1)
	public void plainOldInsertDeleteInsert() {
		plainOldInsertDelete();
		plainOldInsert(DOCUMENTS);
	}
	
	/**
	 * Inserts a lot of documents using MongoMVCC, then deletes them
	 * and inserts another bunch of documents
	 */
	@Test
	@BenchmarkOptions(benchmarkRounds = 2, warmupRounds = 1)
	public void mvccInsertDeleteInsert() {
		mvccInsertDelete();
		mvccInsert(DOCUMENTS);
	}
	
	/**
	 * Queries a lot of objects after inserting, deleting and inserting again
	 */
	@Test
	@BenchmarkOptions(benchmarkRounds = 2, warmupRounds = 1)
	public void plainOldInsertDeleteInsertQuery() {
		plainOldInsertDeleteInsert();
		_master.commit();
		DBCollection coll = _db.getCollection("persons");
		for (DBObject o : coll.find()) {
			assertTrue(((Long)o.get("age")).longValue() >= DOCUMENTS);
		}
	}
	
	/**
	 * Queries a lot of objects after inserting, deleting and inserting again
	 */
	@Test
	@BenchmarkOptions(benchmarkRounds = 2, warmupRounds = 1)
	public void mvccInsertDeleteInsertQuery() {
		mvccInsertDeleteInsert();
		VCollection coll = _master.getCollection("persons");
		for (Map<String, Object> o : coll.find()) {
			assertTrue(((Long)o.get("age")).longValue() >= DOCUMENTS);
		}
	}
}
