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

package de.fhg.igd.mongomvcc;

import java.util.concurrent.TimeUnit;

/**
 * <p>Provides maintenance operations for a MVCC database.</p>
 * <p>Note: some of these operations may take a long time and the database
 * might be locked during this time.</p>
 * <p><strong>Attention: this class contains destructive methods.
 * Using these methods without care may seriously damage your database!
 * Please make sure you absolutely know what you're doing before you call
 * one of these methods.</strong></p> 
 * @author Michel Kraemer
 */
public interface VMaintenance {
	/**
	 * <p>Finds commits which do not belong to a named branch. Only those
	 * commits are considered which are older than the given expiry time (i.e.
	 * a commit which has been added just a minute ago will not be considered
	 * if the expiry time is larger than one minute).</p>
	 * <p><strong>Attention: this method may return commits which are indeed
	 * dangling but which might still be needed by some other thread/process.
	 * Please do not delete such commits from the database if you're unsure
	 * whether they are still needed. A good strategy to avoid such situations
	 * is to use a long expiry time, so only commits which are very old
	 * will be considered.</strong></p>
	 * @param expiry the expiry time. Only those commits older than this
	 * time will be considered.
	 * @param unit the time unit for the expiry argument
	 * @return the dangling commits which are older than the given expiry time
	 */
	long[] findDanglingCommits(long expiry, TimeUnit unit);
	
	/**
	 * <p>Deletes all dangling commits from the database. Only those
	 * commits are considered which are older than the given expiry time (i.e.
	 * a commit which has been added just a minute ago will not be considered
	 * if the expiry time is larger than one minute).</p>
	 * <p><strong>Attention: this is a destructive method. Commits may
	 * seem dangling but they might still be needed by some other
	 * thread/process. Please make sure you're absolutely sure what you are
	 * doing before calling this method. Accidentally deleting non-dangling
	 * commits may leave your database in a broken state! A good strategy
	 * to avoid this is to choose an expiry time that is long enough for
	 * all your transactions.</strong></p>
	 * @param expiry the expiry time. Only those commits older than this
	 * time will be considered.
	 * @param unit the time unit for the expiry argument
	 * @return the number of commits removed
	 */
	long pruneDanglingCommits(long expiry, TimeUnit unit);
	
	/**
	 * <p>Finds documents which do not belong to a commit. Only those
	 * documents are considered which are older than the given expiry time
	 * (i.e. a document that has been added just a minute ago will not be
	 * considered if the expiry time is larger than one minute).</p>
	 * <p><strong>Attention: this method may return documents that seem to be
	 * unreferenced but are still be needed by some other thread/process
	 * that is just about to create a new commit. Please do not delete such
	 * documents from the database if you're unsure whether they are still
	 * needed. A good strategy to avoid such situations is to use a long
	 * expiry time, so only documents which are very old will be
	 * considered.</strong></p>
	 * @param collection the name of the collection to search
	 * @param expiry the expiry time. Only those documents older than this
	 * time will be considered.
	 * @param unit the time unit for the expiry argument
	 * @return the OIDs of unreferenced documents older than the given expiry time
	 */
	long[] findUnreferencedDocuments(String collection, long expiry, TimeUnit unit);
	
	/**
	 * <p>Deletes all unreferenced documents from the database. Only those
	 * documents are considered which are older than the given expiry time
	 * (i.e. a document that has been added just a minute ago will not be
	 * considered if the expiry time is larger than one minute).</p>
	 * <p><strong>Attention: this is a destructive method. Documents may
	 * seem unreferenced but they might still be needed by some other
	 * thread/process which is just about to create a new commit. Please make
	 * sure you're absolutely sure what you are doing before calling this
	 * method. Accidentally deleting referenced documents may leave your
	 * database in a broken state! A good strategy to avoid this is to choose
	 * an expiry time that is long enough for all your transactions.</strong></p>
	 * @param collection the name of the collection to search
	 * @param expiry the expiry time. Only those documents older than this
	 * time will be considered.
	 * @param unit the time unit for the expiry argument
	 * @return the number of documents removed
	 */
	long pruneUnreferencedDocuments(String collection, long expiry, TimeUnit unit);
}
