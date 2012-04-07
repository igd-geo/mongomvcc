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

/**
 * An iterator that wraps around another one, but transforms all elements
 * @author Michel Kraemer
 * @param <I> the type of the input elements
 * @param <O> the type of the output elements
 */
public abstract class TransformingIterator<I, O> implements Iterator<O> {
	/**
	 * The wrapped iterator
	 */
	private final Iterator<I> _delegate;
	
	/**
	 * Constructs a new transforming iterator
	 * @param delegate the iterator to wrap
	 */
	public TransformingIterator(Iterator<I> delegate) {
		_delegate = delegate;
	}
	
	@Override
	public boolean hasNext() {
		return _delegate.hasNext();
	}

	@Override
	public O next() {
		return transform(_delegate.next());
	}
	
	/**
	 * Transforms an element
	 * @param input the element
	 * @return the transformed element
	 */
	abstract protected O transform(I input);

	@Override
	public void remove() {
		_delegate.remove();
	}
}
