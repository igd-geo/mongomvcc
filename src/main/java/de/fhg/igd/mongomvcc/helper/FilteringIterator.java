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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator that wraps around another one but filters
 * its elements using a given filter
 * @author Michel Kraemer
 * @param <T> the type of elements returned by this iterator
 */
public class FilteringIterator<T> implements Iterator<T> {
	/**
	 * The wrapped iterator
	 */
	private final Iterator<T> _delegate;
	
	/**
	 * The filter that shall be applied to all elements
	 * in the wrapped iterator
	 */
	private final Filter<T> _filter;
	
	/**
	 * True if this iterator has been initialized--i.e. if the
	 * first element has been fetched
	 */
	private boolean _initialized = false;
	
	/**
	 * True if this iterator will return another element
	 */
	private boolean _hasNext = false;
	
	/**
	 * The next element that will be returned by {@link #next()}
	 */
	private T _nextElement = null;
	
	/**
	 * Creates a new filtering iterator
	 * @param delegate the iterator to wrap around
	 * @param filter the filter
	 */
	public FilteringIterator(Iterator<T> delegate, Filter<T> filter) {
		_delegate = delegate;
		_filter = filter;
	}
	
	private void advanceToNext() {
		_hasNext = false;
		while (_delegate.hasNext()) {
			T o = _delegate.next();
			if (_filter.filter(o)) {
				_hasNext = true;
				_nextElement = o;
				break;
			}
		}
	}
	
	private void initialize() {
		if (_initialized) {
			return;
		}
		advanceToNext();
		_initialized = true;
	}
	
	@Override
	public boolean hasNext() {
		initialize();
		return _hasNext;
	}

	@Override
	public T next() {
		initialize();
		if (!_hasNext) {
			throw new NoSuchElementException();
		}
		T r = _nextElement;
		advanceToNext();
		return r;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
