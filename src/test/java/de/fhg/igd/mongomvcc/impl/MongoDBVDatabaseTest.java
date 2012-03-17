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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fhg.igd.mongomvcc.VBranch;
import de.fhg.igd.mongomvcc.VCollection;
import de.fhg.igd.mongomvcc.VConstants;
import de.fhg.igd.mongomvcc.VCursor;
import de.fhg.igd.mongomvcc.VDatabase;
import de.fhg.igd.mongomvcc.VException;
import de.fhg.igd.mongomvcc.VFactory;

/**
 * Tests the {@link MongoDBVDatabase}
 * @author Michel Kraemer
 */
public class MongoDBVDatabaseTest {
	private VDatabase _db;
	private VBranch _master;
	private static final VFactory _factory = new MongoDBVFactory();
	
	/**
	 * Before all unit tests run, make sure the database is clean
	 */
	@BeforeClass
	public static void setUpClass() {
		VDatabase db = _factory.createDatabase();
		db.connect("mvcctest");
		db.drop();
	}
	
	/**
	 * Setup test database
	 */
	@Before
	public void setUp() {
		_db = _factory.createDatabase();
		_db.connect("mvcctest");
		_master = _db.checkout(VConstants.MASTER);
	}
	
	/**
	 * Delete test database
	 */
	@After
	public void tearDown() {
		_db.drop();
	}
	
	/**
	 * Put a person into the database
	 * @param name the person's name
	 * @param age the person's age
	 * @return the person (after the put)
	 */
	private Map<String, Object> putPerson(String name, int age) {
		VCollection persons = _master.getCollection("persons");
		assertNotNull(persons);
		Map<String, Object> peter = new HashMap<String, Object>();
		peter.put("name", name);
		peter.put("age", age);
		persons.insert(peter);
		assertNotNull(peter.get("uid"));
		return peter;
	}
	
	/**
	 * Tests if a person can be added into the local index
	 */
	@Test
	public void insertIntoIndex() {
		VCollection persons = _master.getCollection("persons");
		assertNotNull(persons);
		Map<String, Object> peter = putPerson("Peter", 26);
		VCursor c = persons.find();
		assertEquals(1, c.size());
		Map<String, Object> peter2 = c.iterator().next();
		assertEquals(peter, peter2);
	}
	
	/**
	 * Tests if a person can be added to the database, and if other
	 * threads see that change after a commit
	 * @throws Exception if something goes wrong
	 */
	@Test
	public void insertAndCommit() throws Exception {
		putPerson("Peter", 26);
		assertEquals(1, _master.getCollection("persons").find().size());
		
		//index is dirty now. other threads should not be able
		//to see not-committed objects.
		final Integer[] t1result = new Integer[1];
		Thread t1 = new Thread() {
			@Override
			public void run() {
				t1result[0] = _master.getCollection("persons").find().size();
			}
		};
		t1.start();
		t1.join();
		assertEquals(Integer.valueOf(0), t1result[0]);
		
		//commit changes
		_master.commit();
		
		//add another person
		putPerson("Max", 6);
		_master.commit();
		
		//check if other threads see the new object
		final Integer[] t2result = new Integer[1];
		Thread t2 = new Thread() {
			@Override
			public void run() {
				t2result[0] = _master.getCollection("persons").find().size();
			}
		};
		t2.start();
		t2.join();
		assertEquals(Integer.valueOf(2), t2result[0]);
	}
	
	/**
	 * Tests if two threads can work isolated
	 * @throws Exception if something goes wrong
	 */
	@Test
	public void isolation() throws Exception {
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final Integer[] tresult = new Integer[2];
		Thread t = new Thread() {
			@Override
			public void run() {
				VCursor pc = _master.getCollection("persons").find();
				tresult[0] = pc.size();
				Map<String, Object> p1 = pc.iterator().next();
				assertEquals("Peter", p1.get("name"));
				latch2.countDown();
				try {
					latch1.await();
				} catch (InterruptedException e) {
					fail();
					throw new RuntimeException(e);
				}
				VCursor pc2 = _master.getCollection("persons").find();
				tresult[1] = pc2.size();
				Map<String, Object> p2 = pc2.iterator().next();
				assertEquals("Peter", p2.get("name"));
			}
		};
		
		putPerson("Peter", 26);
		_master.commit();
		assertEquals(1, _master.getCollection("persons").find().size());
		
		//check that the other thread sees the first person
		t.start();
		latch2.await();
		assertEquals(Integer.valueOf(1), tresult[0]);
		
		//add another person
		putPerson("Max", 6);
		_master.commit();
		assertEquals(2, _master.getCollection("persons").find().size());
		
		//check that the other thread does not see the second person
		latch1.countDown();
		t.join();
		assertEquals(Integer.valueOf(1), tresult[1]);
	}
	
	/**
	 * Tests if the database throws an exception if a conflict arises
	 * @throws Exception if everything works as expected
	 */
	@Test
	public void conflict() throws Exception {
		//obtain collection to get a snapshot of the current database
		_master.getCollection("persons").find();
		assertEquals(0, _master.getCollection("persons").find().size());

		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		class ConflictCallable implements Callable<Object> {
			@Override
			public Object call() throws Exception {
				//obtain collection to get a snapshot of the current database
				_master.getCollection("persons").find();
				assertEquals(0, _master.getCollection("persons").find().size());

				latch2.countDown();
				try {
					latch1.await();
				} catch (InterruptedException e) {
					fail();
					throw new RuntimeException(e);
				}

				//insert new person and commit
				putPerson("Peter", 26);
				_master.commit();
				assertEquals(1, _master.getCollection("persons").find().size());

				return null;
			}
		}

		//let other thread make commit based on the same head
		FutureTask<Object> ft1 = new FutureTask<Object>(new ConflictCallable());
		Executor exe = Executors.newCachedThreadPool();
		exe.execute(ft1);
		
		//wait until other thread has obtained the same branch
		while (!latch2.await(100, TimeUnit.MILLISECONDS)) {
			//check if the other thread threw an exception meanwhile
			if (ft1.isDone()) {
				ft1.get();
			}
		}

		//let other thread commit its result
		latch1.countDown();

		//insert new person and commit
		putPerson("Max", 5);
		
		//wait for the other thread to exit
		ft1.get();
		
		//should throw here because of a conflict
		try {
			_master.commit();
		} catch (VException e) {
			//this is what we expect
			return;
		}
		
		fail("Should have thrown a VException up to now");
	}
	
	/**
	 * Tests if one object matching a given example can be retrieved from a collection
	 */
	@Test
	public void find() {
		for (int i = 0; i < 20; ++i) {
			putPerson(String.valueOf(i), i);
		}
		_master.commit();
		
		VCursor vc = _master.getCollection("persons").find(_factory.createDocument("name", "10"));
		assertEquals(1, vc.size());
		Map<String, Object> p = vc.iterator().next();
		assertEquals("10", p.get("name"));
		assertEquals(Integer.valueOf(10), p.get("age"));
	}
	
	/**
	 * Tests if a partial object can be retrieved
	 */
	@Test
	public void findFields() {
		putPerson("Peter", 26);
		_master.commit();
		
		VCursor vc = _master.getCollection("persons").find(_factory.createDocument("name", "Peter"), "name");
		assertEquals(1, vc.size());
		Map<String, Object> p = vc.iterator().next();
		assertEquals("Peter", p.get("name"));
		assertNull(p.get("age"));
		assertNotNull(p.get("_id"));
		assertNotNull(p.get("uid"));
		assertEquals(3, p.size()); //name, UID and OID
	}
	
	/**
	 * Tests if multiple objects matching a given example can be retrieved from a collection
	 */
	@Test
	public void findMore() {
		for (int i = 0; i < 20; ++i) {
			putPerson(String.valueOf(i / 2), i / 2);
		}
		_master.commit();
		
		VCursor vc = _master.getCollection("persons").find(_factory.createDocument("name", "5"));
		assertEquals(2, vc.size());
		Iterator<Map<String, Object>> it = vc.iterator();
		Map<String, Object> p1 = it.next();
		assertEquals("5", p1.get("name"));
		assertEquals(Integer.valueOf(5), p1.get("age"));
		Map<String, Object> p2 = it.next();
		assertEquals("5", p2.get("name"));
		assertEquals(Integer.valueOf(5), p2.get("age"));
	}
	
	/**
	 * Tests what happens if a query returns an empty result
	 */
	@Test
	public void findNone() {
		for (int i = 0; i < 20; ++i) {
			putPerson(String.valueOf(i), i);
		}
		_master.commit();
		
		VCursor vc = _master.getCollection("persons").find(_factory.createDocument("name", "100"));
		assertEquals(0, vc.size());
	}
	
	/**
	 * Finds exactly one person
	 */
	@Test
	public void findOne() {
		putPerson("Max", 6);
		putPerson("Peter", 26);
		putPerson("Elvis", 3);
		Map<String, Object> p = _master.getCollection("persons").findOne(
				_factory.createDocument("name", "Peter"));
		assertEquals("Peter", p.get("name"));
		assertEquals(26, p.get("age"));
		
		Map<String, Object> p2 = _master.getCollection("persons").findOne(
				_factory.createDocument("name", "Ulrich"));
		assertNull(p2);
	}
	
	/**
	 * Puts a person into the database and then updates his age
	 * @param commit true if a commit should be made after the put
	 */
	private void update(boolean commit) {
		Map<String, Object> p = putPerson("Peter", 26);
		//p has a UID now. alter it and insert it again
		assertNotNull(p.get("uid"));
		p.put("age", 27); //happy birthday, peter!
		_master.getCollection("persons").insert(p);
		if (commit) {
			_master.commit();
		}
		
		//retrieve peter again and check his age
		VCursor vc = _master.getCollection("persons").find(_factory.createDocument("name", "Peter"));
		assertEquals(1, vc.size());
		Map<String, Object> p2 = vc.iterator().next();
		assertEquals(p, p2);
	}
	
	/**
	 * Calls {@link #update(boolean)} without committing
	 */
	@Test
	public void updateWithoutCommit() {
		update(false);
	}
	
	/**
	 * Calls {@link #update(boolean)} with a commit
	 */
	@Test
	public void updateWithCommit() {
		update(true);
	}
	
	/**
	 * Deletes an object from the index
	 */
	@Test
	public void deleteFromIndex() {
		Map<String, Object> p = putPerson("Peter", 26);
		VCollection persons = _master.getCollection("persons");
		assertEquals(1, persons.find().size());
		persons.delete((Long)p.get("uid"));
		assertEquals(0, persons.find().size());
	}
	
	/**
	 * Deletes an object from the database and tests if other threads
	 * can see this change after a commit
	 * @throws Exception if something goes wrong
	 */
	@Test
	public void delete() throws Exception {
		putPerson("Max", 6);
		putPerson("Elvis", 3); //I knew it!
		Map<String, Object> p = putPerson("Peter", 26);
		_master.commit();
		VCollection persons = _master.getCollection("persons");
		assertEquals(3, persons.find().size());
		persons.delete((Long)p.get("uid"));
		_master.commit();
		assertEquals(2, persons.find().size());
		
		final Integer[] tresult = new Integer[1];
		Thread t = new Thread() {
			@Override
			public void run() {
				VCollection persons = _master.getCollection("persons");
				tresult[0] = persons.find().size();
			}
		};
		t.start();
		t.join();
		assertEquals(Integer.valueOf(2), tresult[0]);
	}
	
	/**
	 * Deletes an object by example
	 * @throws Exception if something goes wrong
	 */
	@Test
	public void deleteByExample() throws Exception {
		putPerson("Max", 6);
		Map<String, Object> p = putPerson("Peter", 26);
		VCollection persons = _master.getCollection("persons");
		assertEquals(2, persons.find().size());
		persons.delete(_factory.createDocument("name", "Max"));
		VCursor ps = persons.find();
		assertEquals(1, ps.size());
		assertEquals(p, ps.iterator().next());
	}
	
	/**
	 * Tests if changes to the index can be rolled back
	 */
	@Test
	public void rollback() {
		VCollection persons = _master.getCollection("persons");
		assertEquals(0, persons.find().size());
		putPerson("Max", 6);
		assertEquals(1, persons.find().size());
		_master.rollback();
		assertEquals(0, persons.find().size());
	}
	
	/**
	 * Tests if large objects can be saved in the database
	 * @throws Exception if something goes wrong
	 */
	@Test
	public void largeObject() throws Exception {
		VCollection coll = _master.getLargeCollection("images");
		byte[] test = new byte[1024 * 1024];
		for (int i = 0; i < test.length; ++i) {
			test[i] = (byte)(i & 0xFF);
		}
		Map<String, Object> obj = new HashMap<String, Object>();
		obj.put("name", "Mona Lisa");
		obj.put("data", test);
		coll.insert(obj);
		
		VCursor vc = coll.find();
		assertEquals(1, vc.size());
		Map<String, Object> obj2 = vc.iterator().next();
		assertEquals("Mona Lisa", obj2.get("name"));
		assertArrayEquals(test, (byte[])obj2.get("data"));
		
		ByteArrayInputStream bais = new ByteArrayInputStream(test);
		obj = new HashMap<String, Object>();
		obj.put("name", "Mona Lisa");
		obj.put("data", bais);
		coll.insert(obj);
		
		Map<String, Object> obj3 = coll.findOne(_factory.createDocument("uid", obj.get("uid")));
		assertEquals("Mona Lisa", obj3.get("name"));
		InputStream is3 = (InputStream)obj3.get("data");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[64 * 1024];
		int read;
		while ((read = is3.read(buf)) > 0) {
			baos.write(buf, 0, read);
		}
		assertArrayEquals(test, baos.toByteArray());
		
		ByteBuffer bb = ByteBuffer.wrap(test);
		obj = new HashMap<String, Object>();
		obj.put("name", "Mona Lisa");
		obj.put("data", bb);
		coll.insert(obj);
		Map<String, Object> obj4 = coll.findOne(_factory.createDocument("uid", obj.get("uid")));
		assertEquals("Mona Lisa", obj4.get("name"));
		ByteBuffer bb4 = (ByteBuffer)obj4.get("data");
		bb4.rewind();
		byte[] test4 = new byte[bb4.remaining()];
		bb4.get(test4);
		assertArrayEquals(test, test4);
	}
	
	/**
	 * Tests if checking out a non-existing branch throws an exception
	 */
	@Test(expected = VException.class)
	public void checkoutNonExistingBranch() {
		_db.checkout("BLABLA");
	}
	
	/**
	 * Tests if checking out a non-existing commit throws an exception
	 */
	@Test(expected = VException.class)
	public void checkoutNonExistingCID() {
		_db.checkout(1234L);
	}
	
	/**
	 * Tests if a previous version of a collection can be checked out
	 * @throws Exception if something goes wrong
	 */
	@Test
	public void goBackInTime() throws Exception {
		Map<String, Object> max = putPerson("Max", 6);
		long oldCid = _master.commit();
		
		VCollection persons = _master.getCollection("persons");
		Map<String, Object> max2 = persons.findOne(_factory.createDocument("name", "Max"));
		assertEquals(6, max2.get("age"));
		
		max.put("age", 7);
		persons.insert(max);
		_master.commit();
		
		persons = _master.getCollection("persons");
		max2 = persons.findOne(_factory.createDocument("name", "Max"));
		assertEquals(7, max2.get("age"));
		
		VBranch oldMaster = _db.checkout(oldCid);
		persons = oldMaster.getCollection("persons");
		max2 = persons.findOne(_factory.createDocument("name", "Max"));
		assertEquals(6, max2.get("age"));
	}
}
