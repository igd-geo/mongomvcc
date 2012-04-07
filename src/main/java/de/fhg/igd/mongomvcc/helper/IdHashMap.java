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
 * <p>Provides an implementation of {@link IdMap} based on a hash table. Uses
 * double hashing. The table size is always a prime number.</p>
 * <p>You MUST NOT try to add {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE}
 * as keys to the map, because these value are reserved for unused table entries.</p>
 * <p>This set is a lot faster than <code>Map&lt;Long, Long&gt;</code> and even
 * a little bit faster than trove4j's implementation.</p>
 * @author Michel Kraemer
 */
public class IdHashMap extends AbstractIdHashCollection implements IdMap {
	/**
	 * Constructs a new map with the default number of
	 * expected elements (DEFAULT_EXPECTED_SIZE) and
	 * the default load factor (DEFAULT_LOAD_FACTOR)
	 */
	public IdHashMap() {
		this(DEFAULT_EXPECTED_SIZE);
	}
	
	/**
	 * Constructs a new map with the given number of
	 * expected elements and the default load
	 * factor (DEFAULT_LOAD_FACTOR)
	 * @param expectedSize the number of expected elements
	 */
	public IdHashMap(int expectedSize) {
		this(expectedSize, DEFAULT_LOAD_FACTOR);
	}
	
	/**
	 * Constructs a new map with the given number of
	 * expected elements and load factor
	 * @param expectedSize the number of expected elements
	 * @param loadFactor the load factor
	 */
	public IdHashMap(int expectedSize, float loadFactor) {
		_overloadFactor = 1 + (1 - loadFactor);
		ensureCapacity(expectedSize);
	}
	
	@Override
	protected long[] ensureCapacity(int elements) {
		long[] oldTable = super.ensureCapacity(elements);
		if (oldTable != null) {
			//copy old values
			for (int i = 0; i < oldTable.length; i += 2) {
				long l = oldTable[i];
				if (l != FREE && l != DELETED) {
					putInternal(l, oldTable[i + 1]);
				}
			}
		}
		return oldTable;
	}
	
	@Override
	protected void makeTable(int capacity) {
		_table = new long[capacity * 2];
		clearArray(_table);
	}
	
	@Override
	protected void clearArray(long[] arr) {
		for (int i = 0; i < arr.length; i += 2) {
			arr[i] = FREE;
		}
	}
	
	/**
	 * Puts a key-value pair into the table (without checks)
	 * @param key the key
	 * @param value the value
	 * @return the old value or 0 if the key was not in the map
	 */
	private long putInternal(long key, long value) {
		int h0 = hash(key);
		int h = h0 % _capacity;
		int i = h * 2;
		if (_table[i] == key) {
			long old = _table[i + 1];
			_table[i + 1] = value;
			return old;
		}
		if (_table[i] != FREE && _table[i] != DELETED) {
			int h1 = 1 + (h0 % (_capacity - 2));
			do {
				h -= h1;
				if (h < 0) {
					h += _capacity;
				}
				i = h * 2;
				if (_table[i] == key) {
					long old = _table[i + 1];
					_table[i + 1] = value;
					return old;
				}
			} while (_table[i] != FREE && _table[i] != DELETED);
		}
		_table[i] = key;
		_table[i + 1] = value;
		return 0;
	}

	@Override
	public long put(long key, long value) {
		if (key == FREE) {
			throw new IllegalArgumentException("Long.MAX_VALUE is not allowed in this map implementation");
		}
		if (key == DELETED) {
			throw new IllegalArgumentException("Long.MIN_VALUE is not allowed in this map implementation");
		}
		long old = putInternal(key, value);
		if (old == 0) {
			++_size;
			ensureCapacity(_size);
		}
		return old;
	}

	@Override
	public boolean containsKey(long key) {
		int h0 = hash(key);
		int h = h0 % _capacity;
		int i = h * 2;
		if (_table[i] == key) {
			return true;
		} else if (_table[i] == FREE) {
			return false;
		}
		
		int h1 = 1 + (h0 % (_capacity - 2));
		while (true) {
			h -= h1;
			if (h < 0) {
				h += _capacity;
			}
			i = h * 2;
			if (_table[i] == key) {
				return true;
			} else if (_table[i] == FREE) {
				return false;
			}
		}
	}

	@Override
	public long remove(long key) {
		int h0 = hash(key);
		int h = h0 % _capacity;
		int i = h * 2;
		if (_table[i] == key) {
			_table[i] = DELETED;
			--_size;
			return _table[i + 1];
		} else if (_table[i] == FREE) {
			return 0;
		}
		
		int h1 = 1 + (h0 % (_capacity - 2));
		while (true) {
			h -= h1;
			if (h < 0) {
				h += _capacity;
			}
			i = h * 2;
			if (_table[i] == key) {
				_table[i] = DELETED;
				--_size;
				return _table[i + 1];
			} else if (_table[i] == FREE) {
				return 0;
			}
		}
	}

	@Override
	public long get(long key) {
		int h0 = hash(key);
		int h = h0 % _capacity;
		int i = h * 2;
		if (_table[i] == key) {
			return _table[i + 1];
		} else if (_table[i] == FREE) {
			return 0;
		}
		
		int h1 = 1 + (h0 % (_capacity - 2));
		while (true) {
			h -= h1;
			if (h < 0) {
				h += _capacity;
			}
			i = h * 2;
			if (_table[i] == key) {
				return _table[i + 1];
			} else if (_table[i] == FREE) {
				return 0;
			}
		}
	}
	
	@Override
	public long[] keys() {
		long[] result = new long[_size];
		int i = 0;
		for (int j = 0; j < _table.length; j += 2) {
			long l = _table[j];
			if (l != FREE && l != DELETED) {
				result[i++] = l;
			}
			if (i == _size) {
				break;
			}
		}
		return result;
	}
	
	@Override
	public long[] values() {
		long[] result = new long[_size];
		int i = 0;
		for (int j = 0; j < _table.length; j += 2) {
			long l = _table[j];
			if (l != FREE && l != DELETED) {
				result[i++] = _table[j + 1];
			}
			if (i == _size) {
				break;
			}
		}
		return result;
	}
	
	@Override
	public IdMapIterator iterator() {
		return new IdMapIterator() {
			private int _n = 0;
			private int _i = -2;
			
			@Override
			public boolean hasNext() {
				return _n < _size;
			}

			@Override
			public void advance() {
				if (_n == _size) {
					throw new NoSuchElementException();
				}
				_i += 2;
				while (_table[_i] == FREE || _table[_i] == DELETED) _i += 2;
				++_n;
			}

			@Override
			public long key() throws NoSuchElementException {
				if (_n > _size || _i < 0) {
					throw new NoSuchElementException();
				}
				return _table[_i];
			}

			@Override
			public long value() throws NoSuchElementException {
				if (_n > _size || _i < 0) {
					throw new NoSuchElementException();
				}
				return _table[_i + 1];
			}
		};
	}
}
