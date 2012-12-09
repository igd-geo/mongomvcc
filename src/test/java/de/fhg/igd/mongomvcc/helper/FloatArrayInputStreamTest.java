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
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;

import org.junit.Test;

/**
 * Tests {@link FloatArrayInputStream}
 * @author Michel Kraemer
 */
public class FloatArrayInputStreamTest {
	/**
	 * Wraps a {@link FloatArrayInputStream} around a simple
	 * array of float values. Then reads the stream and compares
	 * its contents with the original array.
	 * @throws Exception if something goes wrong
	 */
	@Test
	public void simple() throws Exception {
		float[] arr = new float[50000];
		for (int i = 0; i < arr.length; ++i) {
			arr[i] = (float)i;
		}
		
		FloatArrayInputStream fais = new FloatArrayInputStream(arr);
		DataInputStream dis = new DataInputStream(fais);
		for (int i = 0; i < arr.length; ++i) {
			float f = dis.readFloat();
			assertEquals(arr[i], f, 0.00001);
		}
		dis.close();
		assertEquals(-1, dis.read());
	}
	
	/**
	 * Tests if {@link Float#NaN} values can be correctly read
	 * from a {@link FloatArrayInputStream}
	 * @throws Exception if something goes wrong
	 */
	@Test
	public void nan() throws Exception {
		FloatArrayInputStream fais = new FloatArrayInputStream(new float[] { Float.NaN });
		DataInputStream dis = new DataInputStream(fais);
		assertTrue(Float.isNaN(dis.readFloat()));
		assertEquals(-1, dis.read());
		dis.close();
	}
}
