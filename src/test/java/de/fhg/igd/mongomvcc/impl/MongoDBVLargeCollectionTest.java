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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import de.fhg.igd.mongomvcc.VCollection;
import de.fhg.igd.mongomvcc.VCursor;

/**
 * Tests {@link MongoDBVLargeCollection}
 * @author Michel Kraemer
 */
public class MongoDBVLargeCollectionTest extends AbstractMongoDBVDatabaseTest {
	/**
	 * Tests if large objects can be saved in the database
	 * @throws Exception if something goes wrong
	 */
	@Test
	public void largeObject() throws Exception {
		VCollection coll = _master.getLargeCollection("images");
		byte[] test = new byte[1024 * 1024];
		for (int i = 0; i < test.length; ++i) {
			test[i] = (byte)(i & 0xFF);
		}
		Map<String, Object> obj = new HashMap<String, Object>();
		obj.put("name", "Mona Lisa");
		obj.put("data", test);
		coll.insert(obj);
		
		VCursor vc = coll.find();
		assertEquals(1, vc.size());
		Map<String, Object> obj2 = vc.iterator().next();
		assertEquals("Mona Lisa", obj2.get("name"));
		assertArrayEquals(test, (byte[])obj2.get("data"));
		
		ByteArrayInputStream bais = new ByteArrayInputStream(test);
		obj = new HashMap<String, Object>();
		obj.put("name", "Mona Lisa");
		obj.put("data", bais);
		coll.insert(obj);
		
		Map<String, Object> obj3 = coll.findOne(_factory.createDocument("uid", obj.get("uid")));
		assertEquals("Mona Lisa", obj3.get("name"));
		InputStream is3 = (InputStream)obj3.get("data");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[64 * 1024];
		int read;
		while ((read = is3.read(buf)) > 0) {
			baos.write(buf, 0, read);
		}
		assertArrayEquals(test, baos.toByteArray());
		
		ByteBuffer bb = ByteBuffer.wrap(test);
		obj = new HashMap<String, Object>();
		obj.put("name", "Mona Lisa");
		obj.put("data", bb);
		coll.insert(obj);
		Map<String, Object> obj4 = coll.findOne(_factory.createDocument("uid", obj.get("uid")));
		assertEquals("Mona Lisa", obj4.get("name"));
		ByteBuffer bb4 = (ByteBuffer)obj4.get("data");
		bb4.rewind();
		byte[] test4 = new byte[bb4.remaining()];
		bb4.get(test4);
		assertArrayEquals(test, test4);
	}
}
