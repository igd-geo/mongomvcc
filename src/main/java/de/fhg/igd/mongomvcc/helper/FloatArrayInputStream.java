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

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>An input stream that reads from a float array. An internal
 * counter keeps track of the current position in the float
 * array and of the next byte to read. Float values are
 * automatically converted to single bytes using
 * {@link Float#floatToRawIntBits(float)} (default) or
 * {@link Float#floatToIntBits(Float)}.</p>
 * </p>Use a {@link DataInputStream} to read from
 * {@link FloatArrayInputStreams} and to restore the original
 * values.</p>
 * @author Michel Kraemer
 */
public class FloatArrayInputStream extends InputStream {
	/**
	 * The array this input stream wraps around
	 */
	private final float[] _arr;
	
	/**
	 * The current position in the wrapped array
	 */
	private int _pos = 0;
	
	/**
	 * The position of the current byte in the bit representation
	 * of the float value at the current position in the array.
	 */
	private int _subpos = 0;
	
	/**
	 * The bits of the float value at the current position
	 */
	private int _currentBits;
	
	/**
	 * True if {@link Float#floatToRawIntBits(float)} shall be used
	 * to convert float values to bytes or false if {@link Float#floatToIntBits(float)}
	 * shall be used.
	 */
	private final boolean _rawBits;
	
	/**
	 * Creates a new input stream
	 * @param arr the array to wrap
	 */
	public FloatArrayInputStream(float[] arr) {
		this(arr, true);
	}
	
	/**
	 * Creates a new input stream
	 * @param arr the array to wrap
	 * @param rawBits true if {@link Float#floatToRawIntBits(float)} shall be used
	 * to convert float values to bytes or false if {@link Float#floatToIntBits(float)}
	 * shall be used
	 */
	public FloatArrayInputStream(float[] arr, boolean rawBits) {
		_arr = arr;
		_rawBits = rawBits;
	}
	
	@Override
	public int read() throws IOException {
		if (_pos >= _arr.length) {
			//end of stream
			return -1;
		}
		
		if (_subpos == 0) {
			//receive next float value
			_currentBits = makeBits(_arr[_pos]);
		}
		
		//calculate current value
		int s = Float.SIZE - (_subpos + 1) * Byte.SIZE;
		int result = (_currentBits >> s) & 0xff;
		
		//increase position(s)
		++_subpos;
		if (_subpos == Float.SIZE / Byte.SIZE) {
			_subpos = 0;
			++_pos;
		}
		
		return result;
	}
	
	private int makeBits(float f) {
		if (_rawBits) {
			return Float.floatToRawIntBits(f);
		}
		return Float.floatToIntBits(f);
	}
}
