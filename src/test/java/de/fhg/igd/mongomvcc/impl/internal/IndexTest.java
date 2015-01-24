// This file is part of MongoMVCC.
//
// Copyright (c) 2012-2015 Fraunhofer IGD
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

import de.fhg.igd.mongomvcc.VBranch;
import de.fhg.igd.mongomvcc.VCollection;
import de.fhg.igd.mongomvcc.VConstants;
import de.fhg.igd.mongomvcc.VDatabase;
import de.fhg.igd.mongomvcc.impl.AbstractMongoDBVDatabaseTest;
import de.fhg.igd.mongomvcc.impl.MongoDBVDatabase;
import java.util.ArrayList;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

/**
 * Tests {@link de.fhg.igd.mongomvcc.impl.internal.Index}
 * @author Qualtagh
 */
public class IndexTest extends AbstractMongoDBVDatabaseTest {
	/**
	 * This test checks if indexes are loaded in the correct order:
	 * oldest should be first, newest come later.
	 */
	@Test
	public void loadingOrder() {
		VCollection persons = _master.getCollection("persons");

		// add two test objects to 'persons' collection
		Map<String, Object> elvis = _factory.createDocument();
		elvis.put("name", "elvis");
		elvis.put("age", "2");
		persons.insert(elvis);

		Map<String, Object> max = _factory.createDocument();
		max.put("name", "max");
		max.put("age", "3");
		persons.insert(max);

		// save commit and objects currently stored in the database
		long first = _master.commit();
		ArrayList<String> beforeInSession = new ArrayList<String>();
		for (Map<String, Object> person : persons.find()) {
			beforeInSession.add( person.toString() );
		}

		// change an object and insert it again (i.e. update it)
		elvis.put("age", "4");
		persons.insert(elvis);

		// save commit and objects now stored in the database
		long second = _master.commit();
		ArrayList<String> afterInSession = new ArrayList<String>();
		for (Map<String, Object> person : persons.find()) {
			afterInSession.add(person.toString());
		}

		// checkout old commit and save objects stored in the database
		VBranch oldMaster = _db.checkout(first);
		VCollection oldWells = oldMaster.getCollection("persons");
		ArrayList<String> beforeOutOfSession = new ArrayList<String>();
		for (Map<String, Object> person : oldWells.find()) {
			beforeOutOfSession.add(person.toString());
		}

		// checkout new commit and save objects stored in the database
		VBranch newMaster = _db.checkout(second);
		VCollection newWells = newMaster.getCollection("persons");
		ArrayList<String> afterOutOfSession = new ArrayList<String>();
		for (Map<String, Object> person : newWells.find()) {
			afterOutOfSession.add(person.toString());
		}

		// compare stored objects
		assertArrayEquals(beforeInSession.toArray(), beforeOutOfSession.toArray());
		assertArrayEquals(afterInSession.toArray(), afterOutOfSession.toArray());
	}
	
	/**
	 * This test checks if a stack overflows when there are too many commits in the database.
	 * @throws StackOverflowError if the test fails
	 */
	@Test
	public void stackOverflow() throws StackOverflowError {
		VCollection persons = _master.getCollection("stack");
		Map<String, Object> elvis = _factory.createDocument();
		elvis.put("name", "elvis");
		for (int i = 0; i < 2500; i++) {
			persons.insert(elvis);
			_master.commit();
		}

		VDatabase db = _factory.createDatabase();
		db.connect(((MongoDBVDatabase)_db).getDB().getName());
		VBranch master = db.checkout(VConstants.MASTER);
		persons = master.getCollection("stack");
		persons.find().size();
	}
}
