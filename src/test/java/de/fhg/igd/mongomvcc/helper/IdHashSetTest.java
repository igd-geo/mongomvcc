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

package de.fhg.igd.mongomvcc.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * Tests {@link IdHashSet}
 * @author Michel Kraemer
 */
public class IdHashSetTest {
	/**
	 * Tests the {@link IdHashSet#add(long)} method
	 */
	@Test
	public void add() {
		IdSet s = new IdHashSet();
		assertTrue(s.add(4711));
		assertEquals(1, s.size());
		assertTrue(s.contains(4711));
	}
	
	/**
	 * Adds a lot of numbers (more than expected)
	 */
	@Test
	public void addMoreThanExpected() {
		IdSet s = new IdHashSet(10);
		for (int i = 0; i < 20; ++i) {
			assertTrue(s.add(i * i));
		}
		assertEquals(20, s.size());
		for (int i = 0; i < 20; ++i) {
			assertTrue(s.contains(i * i));
		}
	}
	
	/**
	 * Tests what happens if we try to add an item that is
	 * already in the set
	 */
	@Test
	public void addAlreadyExisting() {
		IdSet s = new IdHashSet();
		assertTrue(s.add(4711));
		assertEquals(1, s.size());
		assertTrue(s.contains(4711));
		assertFalse(s.add(4711));
		assertEquals(1, s.size());
		assertTrue(s.contains(4711));
	}
	
	/**
	 * Tests if the set can be converted to an array
	 */
	@Test
	public void toArray() {
		IdSet s = new IdHashSet(20);
		for (int i = 0; i < 20; ++i) {
			s.add(i * i);
		}
		assertEquals(20, s.size());
		long[] a = s.toArray();
		assertEquals(20, a.length);
		Arrays.sort(a);
		for (int i = 0; i < 20; ++i) {
			assertEquals(i * i, a[i]);
		}
	}
	
	/**
	 * Tests if an item can be removed
	 */
	@Test
	public void remove() {
		IdSet s = new IdHashSet();
		s.add(5);
		assertEquals(1, s.size());
		assertTrue(s.remove(5));
		assertEquals(0, s.size());
	}
	
	/**
	 * Tests what happens if we try to remove a non-existent item
	 */
	@Test
	public void removeNonExistent() {
		IdSet s = new IdHashSet();
		s.add(5);
		assertEquals(1, s.size());
		assertFalse(s.remove(8));
		assertEquals(1, s.size());
	}
	
	/**
	 * Tests if several items can be removed
	 */
	@Test
	public void removeSeveral() {
		IdSet s = new IdHashSet(20);
		for (int i = 0; i < 20; ++i) {
			s.add(i * i);
		}
		assertEquals(20, s.size());
		for (int i = 10; i < 20; ++i) {
			s.remove(i * i);
		}
		assertEquals(10, s.size());
		long[] a = s.toArray();
		assertEquals(10, a.length);
		Arrays.sort(a);
		for (int i = 0; i < 10; ++i) {
			assertEquals(i * i, a[i]);
		}
	}
	
	/**
	 * Clears the set
	 */
	@Test
	public void clear() {
		IdSet s = new IdHashSet();
		for (int i = 0; i < 20; ++i) {
			s.add(i * i);
		}
		assertEquals(20, s.size());
		s.clear();
		assertEquals(0, s.size());
		long[] a = s.toArray();
		assertEquals(0, a.length);
		for (int i = 0; i < 20; ++i) {
			assertFalse(s.contains(i * i));
		}
	}
	
	/**
	 * Tests what happens if we try to access an empty set through an iterator
	 */
	@Test(expected = NoSuchElementException.class)
	public void iteratorEmpty() {
		IdSet s = new IdHashSet();
		IdSetIterator i = s.iterator();
		assertFalse(i.hasNext());
		i.next();
	}
	
	/**
	 * Tests the {@link IdHashSet#iterator()} method
	 */
	@Test
	public void iterator() {
		IdSet s = new IdHashSet();
		for (int j = 0; j < 20; ++j) {
			s.add(j * j);
		}
		IdSetIterator i = s.iterator();
		int n = 0;
		long[] a = new long[20];
		while (i.hasNext()) {
			long j = i.next();
			assertTrue(s.contains(j));
			a[n] = j;
			++n;
		}
		assertEquals(20, n);
		Arrays.sort(a);
		for (int j = 0; j < 20; ++j) {
			assertEquals(j * j, a[j]);
		}
	}
}
