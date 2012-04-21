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
}
