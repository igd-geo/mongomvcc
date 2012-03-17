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

package de.fhg.igd.mongomvcc.util;

import java.util.NoSuchElementException;

/**
 * Represents either one value or another
 * @author Michel Kraemer
 * @param <L> the type of the left value
 * @param <R> the type of the right value
 */
public class Either<L, R> {
	/**
	 * True if the left value is set
	 */
	private final boolean _isLeft;
	
	/**
	 * The left value
	 */
	private final L _left;
	
	/**
	 * The right value
	 */
	private final R _right;
	
	/**
	 * Initializes this object with a left value
	 * @param left the value
	 * @param dummy just a dummy value used to resolve type erasure
	 * conflict. You can simply ignore it.
	 */
	public Either(L left, L... dummy) {
		_left = left;
		_right = null;
		_isLeft = true;
	}
	
	/**
	 * Initializes this object with a right value
	 * @param right the value
	 */
	public Either(R right) {
		_left = null;
		_right = right;
		_isLeft = false;
	}
	
	/**
	 * @return true if the left value is set
	 */
	public boolean isLeft() {
		return _isLeft;
	}
	
	/**
	 * @return true if the right value is set
	 */
	public boolean isRight() {
		return !_isLeft;
	}
	
	/**
	 * @return the left value
	 * @throws NoSuchElementException if the left value is not set
	 */
	public L getLeft() {
		if (_isLeft)
			return _left;
		throw new NoSuchElementException("Left value is not set");
	}
	
	/**
	 * @return the right value
	 * @throws NoSuchElementException if the right value is not set
	 */
	public R getRight() {
		if (!_isLeft)
			return _right;
		throw new NoSuchElementException("Right value is not set");
	}
}
