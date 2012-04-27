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

/**
 * Abstract base class for hash-based ID collections
 * @author Michel Kraemer
 */
public abstract class AbstractIdHashCollection implements IdCollection {
	/**
	 * The default number of expected elements in this collection
	 */
	protected static final int DEFAULT_EXPECTED_SIZE = 20;
	
	/**
	 * The default load factor
	 */
	protected static final float DEFAULT_LOAD_FACTOR = 0.5f;
	
	/**
	 * The hash table's minimum capacity
	 */
	protected static final int MINIMUM_CAPACITY = 5;
	
	/**
	 * A free table cell
	 */
	protected static final long FREE = Long.MAX_VALUE;
	
	/**
	 * A deleted table cell
	 */
	protected static final long DELETED = Long.MIN_VALUE;
	
	/**
	 * The number of elements in this collection
	 */
	protected int _size = 0;
	
	/**
	 * The number of cells marked as {@link #DELETED}
	 */
	protected int _deleted = 0;
	
	/**
	 * A factor that will be multiplied with the expected number of
	 * elements in order to calculate the capacity
	 */
	protected float _overloadFactor;
	
	/**
	 * The actual table
	 */
	protected long[] _table;
	
	/**
	 * The {@link #_table}'s capacity
	 */
	protected int _capacity = 0;
	
	/**
	 * Clears the given array--i.e. fills it with {@link #FREE} values
	 * @param arr the array to clear
	 */
	protected void clearArray(long[] arr) {
		//even faster than Arrays.fill()
		for (int i = 0; i < arr.length; ++i) {
			arr[i] = FREE;
		}
	}
	
	/**
	 * Ensures that the table can hold a given number of elements.
	 * Resizes the table if needed.
	 * @param elements the number of expected elements
	 * @return the old table or null if nothing was changed
	 */
	protected long[] ensureCapacity(int elements) {
		int capacity = (int)(elements * _overloadFactor);
		
		if (capacity < _capacity) {
			//already OK
			return null;
		}
		
		if (capacity < MINIMUM_CAPACITY) {
			capacity = MINIMUM_CAPACITY;
		}
		int tld = _capacity << 1;
		if (capacity < tld) {
			capacity = tld;
		}
		
		//capacity should ideally be a prime number
		capacity = Primes.next(capacity);
		_capacity = capacity;
		
		long[] oldTable = _table;
		makeTable(capacity);
		return oldTable;
	}
	
	/**
	 * Creates a new, cleared hash table with the given capacity
	 * @param capacity the capacity
	 */
	protected void makeTable(int capacity) {
		_table = new long[capacity];
		clearArray(_table);
		_deleted = 0;
	}
	
	/**
	 * Calculates the hash for a given value
	 * @param value the value
	 * @return the hash
	 */
	protected int hash(long value) {
		return (int)(value ^ (value >>> 32));
	}
	
	@Override
	public int size() {
		return _size;
	}

	@Override
	public void clear() {
		_size = 0;
		clearArray(_table);
	}
}
