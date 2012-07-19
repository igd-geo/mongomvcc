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

package de.fhg.igd.mongomvcc.impl;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import de.fhg.igd.mongomvcc.VCounter;
import de.fhg.igd.mongomvcc.helper.FloatArrayInputStream;

/**
 * The default convert strategy handles different types of binary data
 * which is stores in a MongoDB GridFS
 * @author Michel Kraemer
 */
public class DefaultConvertStrategy implements ConvertStrategy {
	/**
	 * Binary types
	 */
	private static final int BYTEARRAY = 0;
	private static final int INPUTSTREAM = 1;
	private static final int BYTEBUFFER = 2;
	private static final int FLOATARRAY = 3;
	private static final int FLOATBUFFER = 4;
	
	/**
	 * The metadata attribute that denotes the binary type
	 */
	private static final String BINARY_TYPE = "binary_type";
	
	/**
	 * The MongoDB GridFS storing binary data
	 */
	private final GridFS _gridFS;
	
	/**
	 * A counter to generate replacement OIDs
	 */
	private final VCounter _counter;
	
	/**
	 * The files that have been created by this convert strategy
	 */
	private final List<GridFSInputFile> _convertedFiles = new ArrayList<GridFSInputFile>();
	
	/**
	 * Constructs a new convert strategy
	 * @param gridFS the MongoDB GridFS storing binary data
	 * @param counter a counter to generate replacement OIDs
	 */
	public DefaultConvertStrategy(GridFS gridFS, VCounter counter) {
		_gridFS = gridFS;
		_counter = counter;
	}
	
	/**
	 * @return the files that have been created by this convert strategy
	 */
	public List<GridFSInputFile> getConvertedFiles() {
		return _convertedFiles;
	}
	
	@Override
	public long convert(Object data) {
		GridFSInputFile file;
		if (data instanceof byte[]) {
			file = _gridFS.createFile((byte[])data);
			file.put(BINARY_TYPE, BYTEARRAY);
		} else if (data instanceof float[]) {
			file = _gridFS.createFile(new FloatArrayInputStream((float[])data));
			file.put(BINARY_TYPE, FLOATARRAY);
		} else if (data instanceof InputStream) {
			file = _gridFS.createFile((InputStream)data);
			file.put(BINARY_TYPE, INPUTSTREAM);
		} else if (data instanceof ByteBuffer) {
			ByteBuffer bb = (ByteBuffer)data;
			byte[] buf;
			if (bb.hasArray()) {
				buf = bb.array();
			} else {
				bb.rewind();
				buf = new byte[bb.remaining()];
				bb.get(buf);
			}
			file = _gridFS.createFile(buf);
			file.put(BINARY_TYPE, BYTEBUFFER);
		} else if (data instanceof FloatBuffer) {
			FloatBuffer bb = (FloatBuffer)data;
			float[] buf;
			if (bb.hasArray()) {
				buf = bb.array();
			} else {
				bb.rewind();
				buf = new float[bb.remaining()];
				bb.get(buf);
			}
			file = _gridFS.createFile(new FloatArrayInputStream(buf));
			file.put(BINARY_TYPE, FLOATBUFFER);
		} else {
			return 0;
		}

		long oid = _counter.getNextId();
		file.put(MongoDBVLargeCollection.OID, oid);
		_convertedFiles.add(file);
		return oid;
	}

	@Override
	public Object convert(long oid) throws IOException {
		GridFSDBFile file = _gridFS.findOne(new BasicDBObject(MongoDBVLargeCollection.OID, oid));
		if (file == null) {
			return null;
		}
		int type = (Integer)file.get(BINARY_TYPE);
		Object r;
		if (type == BYTEARRAY) {
			r = toByteArray(file);
		} else if (type == INPUTSTREAM) {
			r = file.getInputStream();
		} else if (type == BYTEBUFFER) {
			r = ByteBuffer.wrap(toByteArray(file));
		} else if (type == FLOATARRAY) {
			r = toFloatArray(file);
		} else if (type == FLOATBUFFER) {
			r = FloatBuffer.wrap(toFloatArray(file));
		} else {
			//no information. simply forward the input stream
			r = file.getInputStream();
		}
		return r;
	}
	
	/**
	 * Converts the contents of a GridFS file to a byte array
	 * @param file the file
	 * @return the byte array
	 * @throws IOException if the file could not be read
	 */
	private byte[] toByteArray(GridFSDBFile file) throws IOException {
		InputStream is = file.getInputStream();
		int len = (int)file.getLength();
		int pos = 0;
		byte[] b = new byte[len];
		while (len > 0) {
			int read = is.read(b, pos, len);
			pos += read;
			len -= read;
		}
		return b;
	}
	
	/**
	 * Converts the contents of a GridFS file to a float array
	 * @param file the file
	 * @return the float array
	 * @throws IOException if the file could not be read
	 */
	private float[] toFloatArray(GridFSDBFile file) throws IOException {
		DataInputStream is = new DataInputStream(new BufferedInputStream(
				file.getInputStream()));
		int len = (int)file.getLength() / (Float.SIZE / Byte.SIZE);
		float[] b = new float[len];
		for (int i = 0; i < len; ++i) {
			b[i] = is.readFloat();
		}
		return b;
	}
}
