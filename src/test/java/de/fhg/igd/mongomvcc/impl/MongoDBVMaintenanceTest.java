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

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import de.fhg.igd.mongomvcc.VBranch;
import de.fhg.igd.mongomvcc.VCollection;

/**
 * Tests {@link MongoDBVMaintenance}
 * @author Michel Kraemer
 */
public class MongoDBVMaintenanceTest extends AbstractMongoDBVDatabaseTest {
	/**
	 * Tests if dangling commits can be found
	 * @throws InterruptedException if the test could not wait
	 * for commits to expire
	 */
	@Test
	public void findDanglingCommits() throws InterruptedException {
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
		
		long[] dc = _db.getMaintenance().findDanglingCommits(0, TimeUnit.MILLISECONDS);
		assertEquals(4, dc.length);
		assertArrayEquals(new long[] { cid5, cid6, cid7, cid8 }, dc);
		
		long[] dc2 = _db.getMaintenance().findDanglingCommits(
				System.currentTimeMillis() - stime - 250, TimeUnit.MILLISECONDS);
		assertEquals(3, dc2.length);
		assertArrayEquals(new long[] { cid5, cid6, cid7 }, dc2);
		
		//this unit test should not run longer than 2 days :-)
		long[] dc3 = _db.getMaintenance().findDanglingCommits(2, TimeUnit.DAYS);
		assertEquals(0, dc3.length);
	}
}
