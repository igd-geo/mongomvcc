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
 * Tests {@link IdHashMap}
 * @author Michel Kraemer
 */
public class IdHashMapTest {
	/**
	 * Tests the {@link IdHashMap#put(long, long)} method
	 */
	@Test
	public void put() {
		IdMap m = new IdHashMap();
		assertEquals(0, m.put(4711, 1111));
		assertEquals(1, m.size());
		assertTrue(m.containsKey(4711));
	}
	
	/**
	 * Inserts a lot of numbers (more than expected)
	 */
	@Test
	public void insertMoreThanExpected() {
		IdMap m = new IdHashMap(10);
		for (int i = 0; i < 20; ++i) {
			assertEquals(0, m.put(i * i, i));
		}
		assertEquals(20, m.size());
		for (int i = 0; i < 20; ++i) {
			assertTrue(m.containsKey(i * i));
		}
	}
	
	/**
	 * Tests what happens if we try to add an item that is
	 * already in the map
	 */
	@Test
	public void addAlreadyExisting() {
		IdMap m = new IdHashMap();
		assertEquals(0, m.put(4711, 1111));
		assertEquals(1, m.size());
		assertTrue(m.containsKey(4711));
		assertEquals(1111, m.put(4711, 2222));
		assertEquals(1, m.size());
		assertTrue(m.containsKey(4711));
	}
	
	/**
	 * Tests if we can get a value from the map
	 */
	@Test
	public void get() {
		IdMap m = new IdHashMap();
		assertEquals(0, m.get(5));
		m.put(5, 1);
		assertEquals(1, m.get(5));
		assertEquals(0, m.get(8));
	}
	
	/**
	 * Tests if the keys can be converted to an array
	 */
	@Test
	public void keys() {
		IdMap m = new IdHashMap(20);
		for (int i = 0; i < 20; ++i) {
			m.put(i * i, i);
		}
		assertEquals(20, m.size());
		long[] a = m.keys();
		assertEquals(20, a.length);
		Arrays.sort(a);
		for (int i = 0; i < 20; ++i) {
			assertEquals(i * i, a[i]);
		}
	}
	
	/**
	 * Tests if the values can be converted to an array
	 */
	@Test
	public void values() {
		IdMap m = new IdHashMap(20);
		for (int i = 0; i < 20; ++i) {
			m.put(i * i, i);
		}
		assertEquals(20, m.size());
		long[] a = m.values();
		assertEquals(20, a.length);
		Arrays.sort(a);
		for (int i = 0; i < 20; ++i) {
			assertEquals(i, a[i]);
		}
	}
	
	/**
	 * Tests if a key can be removed
	 */
	@Test
	public void remove() {
		IdMap m = new IdHashMap();
		m.put(5, 1);
		assertEquals(1, m.size());
		assertEquals(1, m.remove(5));
		assertEquals(0, m.size());
	}
	
	/**
	 * Tests what happens if we try to remove a non-existent item
	 */
	@Test
	public void removeNonExistent() {
		IdMap m = new IdHashMap();
		m.put(5, 1);
		assertEquals(1, m.size());
		assertEquals(0, m.remove(8));
		assertEquals(1, m.size());
	}
	
	/**
	 * Tests if several items can be removed
	 */
	@Test
	public void removeSeveral() {
		IdMap m = new IdHashMap(20);
		for (int i = 0; i < 20; ++i) {
			m.put(i * i, i);
		}
		assertEquals(20, m.size());
		for (int i = 10; i < 20; ++i) {
			m.remove(i * i);
		}
		assertEquals(10, m.size());
		long[] a = m.keys();
		assertEquals(10, a.length);
		Arrays.sort(a);
		for (int i = 0; i < 10; ++i) {
			assertEquals(i * i, a[i]);
		}
	}
	
	/**
	 * Clears the map
	 */
	@Test
	public void clear() {
		IdMap m = new IdHashMap();
		for (int i = 0; i < 20; ++i) {
			m.put(i * i, i);
		}
		assertEquals(20, m.size());
		m.clear();
		assertEquals(0, m.size());
		long[] a = m.keys();
		assertEquals(0, a.length);
		for (int i = 0; i < 20; ++i) {
			assertFalse(m.containsKey(i * i));
		}
	}
	
	/**
	 * Tests what happens if we try to access an empty map through an iterator
	 */
	@Test(expected = NoSuchElementException.class)
	public void iteratorEmpty() {
		IdMap m = new IdHashMap();
		IdMapIterator i = m.iterator();
		assertFalse(i.hasNext());
		i.advance();
	}
	
	/**
	 * Tests the {@link IdHashMap#iterator()} method
	 */
	@Test
	public void iterator() {
		IdMap m = new IdHashMap();
		for (int j = 1; j < 21; ++j) {
			m.put(j * j, j);
		}
		IdMapIterator i = m.iterator();
		int n = 0;
		long[] ak = new long[20];
		long[] av = new long[20];
		while (i.hasNext()) {
			i.advance();
			long k = i.key();
			long v = i.value();
			assertTrue(m.containsKey(k));
			assertEquals(v * v, k);
			ak[n] = k;
			av[n] = v;
			++n;
		}
		assertEquals(20, n);
		Arrays.sort(ak);
		Arrays.sort(av);
		for (int j = 1; j < 21; ++j) {
			assertEquals(j * j, ak[j - 1]);
			assertEquals(j, av[j - 1]);
		}
	}
}
