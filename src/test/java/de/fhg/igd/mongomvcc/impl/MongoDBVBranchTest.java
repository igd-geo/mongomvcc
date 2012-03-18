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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;

import de.fhg.igd.mongomvcc.VBranch;
import de.fhg.igd.mongomvcc.VCollection;
import de.fhg.igd.mongomvcc.VConstants;
import de.fhg.igd.mongomvcc.VException;

/**
 * Tests {@link MongoDBVBranch}
 * @author Michel Kraemer
 */
public class MongoDBVBranchTest extends AbstractMongoDBVDatabaseTest {
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
	
	/**
	 * Tries to create a branch with a name already used
	 */
	@Test(expected = VException.class)
	public void createExistingBranch() {
		_db.createBranch(VConstants.MASTER, _master.getHead());
	}
	
	/**
	 * Tries to create a branch pointing to a non-existent commit
	 */
	@Test(expected = VException.class)
	public void createBranchInvalidCID() {
		_db.createBranch("BLABLA", _master.getHead() + 1000L);
	}
	
	/**
	 * Creates another branch and tests its isolation against the master branch
	 */
	@Test
	public void branch() {
		putPerson("Peter", 26);
		long peterCid = _master.commit();
		
		VBranch maxBranch = _db.createBranch("Max", peterCid);
		VCollection maxPersons = maxBranch.getCollection("persons");
		assertEquals(1, maxPersons.find().size());
		
		maxPersons.insert(_factory.createDocument("name", "Max"));
		long maxCid = maxBranch.commit();
		
		maxBranch = _db.checkout("Max");
		assertEquals(maxCid, maxBranch.getHead());
		
		VBranch peterBranch = _db.checkout(VConstants.MASTER);
		assertEquals(peterCid, peterBranch.getHead());
		
		maxPersons = maxBranch.getCollection("persons");
		VCollection peterPersons = peterBranch.getCollection("persons");
		assertEquals(2, maxPersons.find().size());
		assertEquals(1, peterPersons.find().size());
		assertNotNull(maxPersons.findOne(_factory.createDocument("name", "Max")));
		assertNull(peterPersons.findOne(_factory.createDocument("name", "Max")));
		
		putPerson("Elvis", 3);
		long elvisCid = _master.commit();
		
		maxBranch = _db.checkout("Max");
		peterBranch = _db.checkout(VConstants.MASTER);
		assertEquals(maxCid, maxBranch.getHead());
		assertEquals(elvisCid, peterBranch.getHead());
		maxPersons = maxBranch.getCollection("persons");
		peterPersons = peterBranch.getCollection("persons");
		assertEquals(2, maxPersons.find().size());
		assertEquals(2, peterPersons.find().size());
		assertNotNull(maxPersons.findOne(_factory.createDocument("name", "Max")));
		assertNull(peterPersons.findOne(_factory.createDocument("name", "Max")));
		assertNotNull(peterPersons.findOne(_factory.createDocument("name", "Elvis")));
		assertNull(maxPersons.findOne(_factory.createDocument("name", "Elvis")));
	}
	
	/**
	 * Creates another branch after a conflict has happened
	 */
	@Test
	public void createBranchAfterConflict() {
		VBranch master2 = _db.checkout(VConstants.MASTER);
		putPerson("Max", 3);

		VCollection persons2 = master2.getCollection("persons");
		persons2.insert(_factory.createDocument("name", "Elvis"));

		long masterCid = _master.commit();
		try {
			master2.commit();
			fail("We expect a VException here since the branch's head " +
					"could not be updated");
		} catch (VException e) {
			//this is what we expect here
		}
		
		//committing master2 failed, but the commit is still there
		long master2Cid = master2.getHead();
		assertTrue(masterCid != master2Cid);
		_db.createBranch("master2", master2Cid);
		
		VBranch master = _db.checkout(VConstants.MASTER);
		assertEquals(masterCid, master.getHead());
		master2 = _db.checkout("master2");
		assertEquals(master2Cid, master2.getHead());
	}
}
