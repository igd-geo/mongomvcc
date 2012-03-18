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

package de.fhg.igd.mongomvcc.impl.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.fhg.igd.mongomvcc.VBranch;
import de.fhg.igd.mongomvcc.VCollection;
import de.fhg.igd.mongomvcc.VException;
import de.fhg.igd.mongomvcc.VHistory;
import de.fhg.igd.mongomvcc.impl.AbstractMongoDBVDatabaseTest;

/**
 * Tests the database {@link Tree}
 * @author Michel Kraemer
 */
public class TreeTest extends AbstractMongoDBVDatabaseTest {
	/**
	 * Tests if the history works correctly
	 */
	@Test
	public void history() {
		long root = _master.getHead();
		putPerson("Max", 3);
		long c1 = _master.commit();
		putPerson("Peter", 26);
		long c2 = _master.commit();
		
		VHistory h = _db.getHistory();
		assertEquals(c1, h.getParent(c2));
		assertEquals(root, h.getParent(c1));
		assertEquals(0, h.getParent(root));
		
		assertArrayEquals(new long[] { root }, h.getChildren(0));
		assertArrayEquals(new long[] { c1 }, h.getChildren(root));
		assertArrayEquals(new long[] { c2 }, h.getChildren(c1));
		assertArrayEquals(new long[0], h.getChildren(c2));
		
		VBranch master2 = _db.createBranch("master2", c1);
		VCollection persons = master2.getCollection("persons");
		persons.insert(_factory.createDocument("name", "Elvis"));
		long c3 = master2.commit();
		
		h = _db.getHistory();
		assertEquals(c1, h.getParent(c2));
		assertEquals(c1, h.getParent(c3));
		assertEquals(root, h.getParent(c1));
		assertEquals(0, h.getParent(root));
		
		assertArrayEquals(new long[] { root }, h.getChildren(0));
		assertArrayEquals(new long[] { c1 }, h.getChildren(root));
		long[] c1c = h.getChildren(c1);
		assertEquals(2, c1c.length);
		assertTrue((c1c[0] == c2 && c1c[1] == c3) || (c1c[0] == c3 && c1c[1] == c2));
		assertArrayEquals(new long[0], h.getChildren(c2));
		assertArrayEquals(new long[0], h.getChildren(c3));
	}
	
	/**
	 * Tests if the history throws an exception if we try to
	 * resolve a non-existent commit
	 */
	@Test(expected = VException.class)
	public void historyThrowNonExistentCommit1() {
		_db.getHistory().getParent(0);
	}
	
	/**
	 * Tests if the history throws an exception if we try to
	 * resolve a non-existent commit
	 */
	@Test(expected = VException.class)
	public void historyThrowNonExistentCommit2() {
		_db.getHistory().getChildren(1000L);
	}
}
