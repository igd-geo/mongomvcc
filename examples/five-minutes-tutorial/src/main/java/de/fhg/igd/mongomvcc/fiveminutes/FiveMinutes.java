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

package de.fhg.igd.mongomvcc.fiveminutes;

import java.util.Map;

import de.fhg.igd.mongomvcc.VBranch;
import de.fhg.igd.mongomvcc.VCollection;
import de.fhg.igd.mongomvcc.VConstants;
import de.fhg.igd.mongomvcc.VCursor;
import de.fhg.igd.mongomvcc.VDatabase;
import de.fhg.igd.mongomvcc.VFactory;
import de.fhg.igd.mongomvcc.impl.MongoDBVFactory;

/**
 * Demonstrates the use of MongoMVCC in 5 minutes
 * @author Michel Kraemer
 */
public class FiveMinutes {
	/**
	 * Runs the tutorial
	 * @param args the program arguments
	 */
	public static void main(String[] args) {
		// 1. Connect to a database
		VFactory factory = new MongoDBVFactory();
		VDatabase db = factory.createDatabase();
		db.connect("mongomvcc-five-minutes-tutorial");
		
		// Checkout the "master" branch
		VBranch master = db.checkout(VConstants.MASTER);
		
		// 2. Put something into the index
		VCollection persons = master.getCollection("persons");
		Map<String, Object> elvis = factory.createDocument();
		elvis.put("name", "Elvis");
		elvis.put("age", 3);
		persons.insert(elvis);
		
		// insert another person
		persons.insert(factory.createDocument("name", "Peter"));
		
		// 3. Commit index to the database
		master.commit();
		
		// 4. Read documents from the database
		VCursor c = persons.find();
		for (Map<String, Object> person : c) {
			System.out.print("Person { name: " + person.get("name"));
			if (person.containsKey("age")) {
				 System.out.print(", age: " + person.get("age"));
			}
			System.out.println(" }");
		}
		
		Map<String, Object> elvis2 = persons.findOne(factory.createDocument("name", "Elvis"));
		if (elvis2 != null) {
			System.out.println("Elvis lives!");
		}
		
		// 5. Make another commit
		persons.insert(factory.createDocument("name", "Max"));
		master.commit();
		
		// 6. Checkout a previous version
		// TODO not implemented yet
		
		// 7. Drop the database
		db.drop();
	}
}
