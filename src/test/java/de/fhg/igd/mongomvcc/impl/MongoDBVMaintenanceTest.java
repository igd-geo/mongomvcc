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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;

import de.fhg.igd.mongomvcc.VBranch;
import de.fhg.igd.mongomvcc.VCollection;
import de.fhg.igd.mongomvcc.VException;
import de.fhg.igd.mongomvcc.impl.internal.MongoDBConstants;

/**
 * Tests {@link MongoDBVMaintenance}
 * @author Michel Kraemer
 */
public class MongoDBVMaintenanceTest extends AbstractMongoDBVDatabaseTest {
	private Object[] makeDanglingCommits() throws InterruptedException {
		putPerson("Max", 6);
		long cid1 = _master.commit();
		VBranch master2 = _db.createBranch("master2", cid1);
		putPerson("Elvis", 3);
		long cid2 = _master.commit();
		VBranch master3 = _db.createBranch("master3", cid2);
		
		VCollection persons2 = master2.getCollection("persons");
		persons2.insert(_factory.createDocument("name", "Pax"));
		long cid3 = master2.commit();
		
		VCollection persons3 = master3.getCollection("persons");
		persons3.insert(_factory.createDocument("name", "Peter"));
		long cid4 = master3.commit();
		
		VBranch master1a = _db.checkout(cid2);
		VCollection persons1a = master1a.getCollection("persons");
		persons1a.insert(_factory.createDocument("name", "Tom"));
		long cid5 = master1a.commit();
		
		VBranch master2a = _db.checkout(cid3);
		VCollection persons2a = master2a.getCollection("persons");
		persons2a.insert(_factory.createDocument("name", "Bob"));
		long cid6 = master2a.commit();
		
		VBranch master3a = _db.checkout(cid4);
		VCollection persons3a = master3a.getCollection("persons");
		persons3a.insert(_factory.createDocument("name", "Howard"));
		long cid7 = master3a.commit();
		
		long stime = System.currentTimeMillis();
		Thread.sleep(500);
		
		persons3a.insert(_factory.createDocument("name", "Brenda"));
		long cid8 = master3a.commit();
		
		return new Object[] { new long[] { cid5, cid6, cid7, cid8 }, stime };
	}
	
	/**
	 * Tests if dangling commits can be found
	 * @throws InterruptedException if the test could not wait
	 * for commits to expire
	 */
	@Test
	public void findDanglingCommits() throws InterruptedException {
		Object[] oa = makeDanglingCommits();
		long[] dangling = (long[])oa[0];
		long stime = (Long)oa[1];
		
		long[] dc = _db.getMaintenance().findDanglingCommits(0, TimeUnit.MILLISECONDS);
		assertEquals(4, dc.length);
		assertArrayEquals(dangling, dc);
		
		long[] dc2 = _db.getMaintenance().findDanglingCommits(
				System.currentTimeMillis() - stime - 250, TimeUnit.MILLISECONDS);
		assertEquals(3, dc2.length);
		assertArrayEquals(new long[] { dangling[0], dangling[1], dangling[2] }, dc2);
		
		//this unit test should not run longer than 2 days :-)
		long[] dc3 = _db.getMaintenance().findDanglingCommits(2, TimeUnit.DAYS);
		assertEquals(0, dc3.length);
	}
	
	/**
	 * Tests if dangling commits can be deleted
	 * @throws InterruptedException if the test could not wait
	 * for commits to expire
	 */
	@Test
	public void pruneDanglingCommits() throws InterruptedException {
		Object[] oa = makeDanglingCommits();
		long[] dangling = (long[])oa[0];
		
		long count = _db.getMaintenance().pruneDanglingCommits(0, TimeUnit.MILLISECONDS);
		assertEquals(4, count);
		
		for (int i = 0; i < 4; ++i) {
			try {
				_db.checkout(dangling[i]);
				fail();
			} catch(VException e) { /* ignore */ }
		}
	}
	
	private Object[] makeUnreferencedDocuments() throws InterruptedException {
		long[] r = new long[3];
		
		putPerson("Max", 6);
		long cid1 = _master.commit();
		putPerson("Brenda", 40);
		_master.commit();
		Map<String, Object> elvis = putPerson("Elvis", 3);
		r[0] = (Long)elvis.get(MongoDBConstants.ID);
		
		VBranch master2 = _db.createBranch("master2", cid1);
		VCollection persons2 = master2.getCollection("persons");
		persons2.insert(_factory.createDocument("name", "Howard"));
		master2.commit();
		Map<String, Object> fritz = _factory.createDocument("name", "Fritz");
		persons2.insert(fritz);
		r[1] = (Long)fritz.get(MongoDBConstants.ID);
		
		long stime = System.currentTimeMillis();
		Thread.sleep(500);
		
		Map<String, Object> david = _factory.createDocument("name", "David");
		persons2.insert(david);
		r[2] = (Long)david.get(MongoDBConstants.ID);
		
		return new Object[] { r, stime };
	}
	
	/**
	 * Tests if unreferenced documents can be found
	 * @throws InterruptedException if the test could not wait
	 * for documents to expire
	 */
	@Test
	public void findUnreferencedDocuments() throws InterruptedException {
		Object[] ud = makeUnreferencedDocuments();
		long[] unreferenced = (long[])ud[0];
		long stime = (Long)ud[1];
		
		long[] fu = _db.getMaintenance().findUnreferencedDocuments("persons", 0, TimeUnit.MILLISECONDS);
		Arrays.sort(fu);
		assertEquals(3, fu.length);
		assertArrayEquals(unreferenced, fu);
		
		long[] fu2 = _db.getMaintenance().findUnreferencedDocuments("persons",
				System.currentTimeMillis() - stime - 250, TimeUnit.MILLISECONDS);
		Arrays.sort(fu2);
		assertEquals(2, fu2.length);
		assertArrayEquals(new long[] { unreferenced[0], unreferenced[1] }, fu2);
		
		//this unit test should not run longer than 2 days :-)
		long[] fu3 = _db.getMaintenance().findUnreferencedDocuments("persons", 2, TimeUnit.DAYS);
		assertEquals(0, fu3.length);
	}
	
	/**
	 * Tests if unreferenced documents can be deleted
	 * @throws Exception if something goes wrong
	 */
	@Test
	public void pruneUnreferencedDocuments() throws Exception {
		Object[] ud = makeUnreferencedDocuments();
		long[] unreferenced = (long[])ud[0];
		
		long count = _db.getMaintenance().pruneUnreferencedDocuments("persons",
				0, TimeUnit.MILLISECONDS);
		assertEquals(3, count);
		
		Mongo mongo = new Mongo();
		DB db = mongo.getDB("mvcctest");
		DBCollection personsColl = db.getCollection("persons");
		for (int i = 0; i < 3; ++i) {
			DBCursor cursor = personsColl.find(new BasicDBObject(MongoDBConstants.ID, unreferenced[i]));
			assertEquals(0, cursor.size());
		}
		assertEquals(3, personsColl.find().size());
	}
}
