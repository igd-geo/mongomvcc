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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * Tests {@link FilteringIterator}
 * @author Michel Kraemer
 */
public class FilteringIteratorTest {
	/**
	 * Tests what happens if we try to filter an empty iterator
	 */
	@Test(expected = NoSuchElementException.class)
	public void emptyDelegate() {
		Iterator<Integer> ti = new FilteringIterator<Integer>(
				Collections.<Integer>emptyList().iterator(), new Filter<Integer>() {
			@Override
			public boolean filter(Integer t) {
				return true;
			}
		});
		assertFalse(ti.hasNext());
		ti.next();
	}
	
	/**
	 * Tests what happens if we filter out all elements
	 */
	@Test
	public void empty() {
		List<Integer> ints = new ArrayList<Integer>();
		for (int i = 0; i < 20; ++i) {
			ints.add(i);
		}
		Iterator<Integer> ti = new FilteringIterator<Integer>(ints.iterator(),
				new Filter<Integer>() {
			@Override
			public boolean filter(Integer t) {
				return false;
			}
		});
		assertFalse(ti.hasNext());
	}
	
	/**
	 * Filter out odd numbers
	 */
	@Test
	public void odd() {
		List<Integer> ints = new ArrayList<Integer>();
		for (int i = 0; i < 20; ++i) {
			ints.add(i);
		}
		Iterator<Integer> ti = new FilteringIterator<Integer>(ints.iterator(),
				new Filter<Integer>() {
			@Override
			public boolean filter(Integer t) {
				return t % 2 == 0;
			}
		});
		for (int i = 0; i < 10; ++i) {
			assertTrue(ti.hasNext());
			assertEquals(i * 2, (int)ti.next());
		}
		assertFalse(ti.hasNext());
	}
	
	/**
	 * Filter out even numbers
	 */
	@Test
	public void even() {
		List<Integer> ints = new ArrayList<Integer>();
		for (int i = 0; i < 20; ++i) {
			ints.add(i);
		}
		Iterator<Integer> ti = new FilteringIterator<Integer>(ints.iterator(),
				new Filter<Integer>() {
			@Override
			public boolean filter(Integer t) {
				return t % 2 != 0;
			}
		});
		for (int i = 0; i < 10; ++i) {
			assertTrue(ti.hasNext());
			assertEquals(i * 2 + 1, (int)ti.next());
		}
		assertFalse(ti.hasNext());
	}
	
	/**
	 * Checks if the remove method really throws an exception
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void remove() {
		List<Integer> ints = new ArrayList<Integer>();
		for (int i = 0; i < 20; ++i) {
			ints.add(i);
		}
		Iterator<Integer> ti = new FilteringIterator<Integer>(ints.iterator(),
				new Filter<Integer>() {
			@Override
			public boolean filter(Integer t) {
				return true;
			}
		});
		ti.next();
		ti.remove();
	}
}
