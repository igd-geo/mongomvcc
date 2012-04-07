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

import java.util.NoSuchElementException;

/**
 * <p>Provides an implementation of {@link IdSet} based on a hash table. Uses
 * double hashing. The table size is always a prime number.</p>
 * <p>You MUST NOT try to add {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE} to the
 * set, because these value are reserved for unused table entries.</p>
 * <p>This set is a lot faster than <code>List&lt;Long&gt;</code> and even
 * a little bit faster than trove4j's implementation.</p>
 * @author Michel Kraemer
 */
public class IdHashSet extends AbstractIdHashCollection implements IdSet {
	/**
	 * Constructs a new set with the default number of
	 * expected elements (DEFAULT_EXPECTED_SIZE) and
	 * the default load factor (DEFAULT_LOAD_FACTOR)
	 */
	public IdHashSet() {
		this(DEFAULT_EXPECTED_SIZE);
	}
	
	/**
	 * Constructs a new set with the given number of
	 * expected elements and the default load
	 * factor (DEFAULT_LOAD_FACTOR)
	 * @param expectedSize the number of expected elements
	 */
	public IdHashSet(int expectedSize) {
		this(expectedSize, DEFAULT_LOAD_FACTOR);
	}
	
	/**
	 * Constructs a new set with the given number of
	 * expected elements and load factor
	 * @param expectedSize the number of expected elements
	 * @param loadFactor the load factor
	 */
	public IdHashSet(int expectedSize, float loadFactor) {
		_overloadFactor = 1 + (1 - loadFactor);
		ensureCapacity(expectedSize);
	}
	
	@Override
	protected long[] ensureCapacity(int elements) {
		long[] oldTable = super.ensureCapacity(elements);
		if (oldTable != null) {
			//copy old values
			for (long l : oldTable) {
				if (l != FREE && l != DELETED) {
					addInternal(l);
				}
			}
		}
		return oldTable;
	}
	
	/**
	 * Adds a value to the table (without checks)
	 * @param value the value
	 * @return true if the value has been added, false if it was already
	 * in the table
	 */
	private boolean addInternal(long value) {
		int h0 = hash(value);
		int h = h0 % _capacity;
		if (_table[h] == value) {
			return false;
		}
		if (_table[h] != FREE && _table[h] != DELETED) {
			int h1 = 1 + (h0 % (_capacity - 2));
			do {
				h -= h1;
				if (h < 0) {
					h += _capacity;
				}
				if (_table[h] == value) {
					return false;
				}
			} while (_table[h] != FREE && _table[h] != DELETED);
		}
		_table[h] = value;
		return true;
	}

	@Override
	public boolean add(long value) {
		if (value == FREE) {
			throw new IllegalArgumentException("Long.MAX_VALUE is not allowed in this set implementation");
		}
		if (value == DELETED) {
			throw new IllegalArgumentException("Long.MIN_VALUE is not allowed in this set implementation");
		}
		if (addInternal(value)) {
			++_size;
			ensureCapacity(_size);
			return true;
		}
		return false;
	}

	@Override
	public boolean contains(long value) {
		int h0 = hash(value);
		int h = h0 % _capacity;
		if (_table[h] == value) {
			return true;
		} else if (_table[h] == FREE) {
			return false;
		}
		
		int h1 = 1 + (h0 % (_capacity - 2));
		while (true) {
			h -= h1;
			if (h < 0) {
				h += _capacity;
			}
			if (_table[h] == value) {
				return true;
			} else if (_table[h] == FREE) {
				return false;
			}
		}
	}

	@Override
	public boolean remove(long value) {
		int h0 = hash(value);
		int h = h0 % _capacity;
		if (_table[h] == value) {
			_table[h] = DELETED;
			--_size;
			return true;
		} else if (_table[h] == FREE) {
			return false;
		}
		
		int h1 = 1 + (h0 % (_capacity - 2));
		while (true) {
			h -= h1;
			if (h < 0) {
				h += _capacity;
			}
			if (_table[h] == value) {
				_table[h] = DELETED;
				--_size;
				return true;
			} else if (_table[h] == FREE) {
				return false;
			}
		}
	}

	@Override
	public long[] toArray() {
		long[] result = new long[_size];
		int i = 0;
		for (long l : _table) {
			if (l != FREE && l != DELETED) {
				result[i++] = l;
			}
		}
		return result;
	}
	
	@Override
	public IdSetIterator iterator() {
		return new IdSetIterator() {
			private int _n = 0;
			private int _i = 0;
			
			@Override
			public boolean hasNext() {
				return _n < _size;
			}

			@Override
			public long next() throws NoSuchElementException {
				if (_n == _size) {
					throw new NoSuchElementException();
				}
				while (_table[_i] == FREE || _table[_i] == DELETED) ++_i;
				++_n;
				return _table[_i++];
			}
		};
	}
}
