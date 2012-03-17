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

package de.fhg.igd.mongomvcc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * Tests {@link Either}
 * @author Michel Kraemer
 */
public class EitherTest {
	/**
	 * Tests if the left value can be set
	 */
	@Test
	public void left() {
		Either<String, Integer> e = new Either<String, Integer>("Hello");
		assertTrue(e.isLeft());
		assertFalse(e.isRight());
		assertEquals("Hello", e.getLeft());
	}
	
	/**
	 * Tests if the right value can be set
	 */
	@Test
	public void right() {
		Either<String, Integer> e = new Either<String, Integer>(Integer.valueOf(5));
		assertFalse(e.isLeft());
		assertTrue(e.isRight());
		assertEquals(Integer.valueOf(5), e.getRight());
	}
	
	/**
	 * Tests if {@link Either#getRight()} throws an exception if the left value is set
	 */
	@Test(expected = NoSuchElementException.class)
	public void throwOnGetRight() {
		Either<String, Integer> e = new Either<String, Integer>("Hello");
		e.getRight();
	}
	
	/**
	 * Tests if {@link Either#getLeft()} throws an exception if the right value is set
	 */
	@Test(expected = NoSuchElementException.class)
	public void throwOnGetLeft() {
		Either<String, Integer> e = new Either<String, Integer>(Integer.valueOf(5));
		e.getLeft();
	}
}
