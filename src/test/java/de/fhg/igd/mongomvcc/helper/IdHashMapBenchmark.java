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

import static org.junit.Assert.assertTrue;
import gnu.trove.map.hash.TLongLongHashMap;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;

/**
 * Benchmarks {@link IdHashMap} compared to the implementations of
 * Java and trove4j
 * @author Michel Kraemer
 */
@Ignore
public class IdHashMapBenchmark extends AbstractBenchmark {
	private static final long[] values = new long[500000];
	
	/**
	 * Set up an array of random values
	 */
	@BeforeClass
	public static void setUpClass() {
		for (int i = 0; i < values.length; ++i) {
			values[i] = (long)(Math.random() * values.length);
		}
	}
	
	/**
	 * Test the java implementation
	 */
	@Test
	public void javaLongMap() {
		Map<Long, Long> map = new HashMap<Long, Long>(values.length * 3 / 2);
		for (int i = 0; i < values.length; ++i) {
			map.put(values[i], values[values.length - i - 1]);
		}
		
		for (int i = 0; i < values.length; ++i) {
			assertTrue(map.containsKey(values[i]));
		}
		
		for (int i = 0; i < values.length; ++i) {
			map.remove(values[i]);
		}
	}
	
	/**
	 * Test trove4j's implementation
	 */
	@Test
	public void troveLongMap() {
		TLongLongHashMap map = new TLongLongHashMap(values.length * 3 / 2);
		for (int i = 0; i < values.length; ++i) {
			map.put(values[i], values[values.length - i - 1]);
		}
		
		for (int i = 0; i < values.length; ++i) {
			assertTrue(map.containsKey(values[i]));
		}
		
		for (int i = 0; i < values.length; ++i) {
			map.remove(values[i]);
		}
	}
	
	/**
	 * Test our implementation
	 */
	@Test
	public void idMap() {
		IdMap map = new IdHashMap(values.length);
		for (int i = 0; i < values.length; ++i) {
			map.put(values[i], values[values.length - i - 1]);
		}
		
		for (int i = 0; i < values.length; ++i) {
			assertTrue(map.containsKey(values[i]));
		}
		
		for (int i = 0; i < values.length; ++i) {
			map.remove(values[i]);
		}
	}
}
