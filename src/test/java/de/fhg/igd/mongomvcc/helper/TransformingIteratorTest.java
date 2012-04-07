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
 * Tests {@link TransformingIterator}
 * @author Michel Kraemer
 */
public class TransformingIteratorTest {
	/**
	 * Tests if a list of integers can be multiplied by 2
	 */
	@Test
	public void multiplyByTwo() {
		List<Integer> ints = new ArrayList<Integer>();
		for (int i = 0; i < 20; ++i) {
			ints.add(i);
		}
		Iterator<Integer> ti = new TransformingIterator<Integer, Integer>(ints.iterator()) {
			@Override
			protected Integer transform(Integer input) {
				return input * 2;
			}
		};
		for (int i = 0; i < 20; ++i) {
			assertTrue(ti.hasNext());
			assertEquals(i * 2, (int)ti.next());
		}
		assertFalse(ti.hasNext());
	}
	
	/**
	 * Tests what happens if we try to transform an empty iterator
	 */
	@Test(expected = NoSuchElementException.class)
	public void empty() {
		Iterator<Integer> ti = new TransformingIterator<Integer, Integer>(Collections.<Integer>emptyList().iterator()) {
			@Override
			protected Integer transform(Integer input) {
				return null;
			}
		};
		assertFalse(ti.hasNext());
		ti.next();
	}
}
