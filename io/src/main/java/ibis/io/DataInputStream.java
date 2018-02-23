/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.io;

import java.io.IOException;

/**
 * A general data input stream.
 * Provides for methods to read data from an underlying implementation.
 * Calls to read functions may block until data is available.
 */
public abstract class DataInputStream extends java.io.InputStream
        implements DataInput {

    public abstract void close() throws IOException;

    /**
     * Returns the number of bytes read from the stream 
     * since the last reset of this counter.
     * @return The number of bytes read.
     */
    public abstract long bytesRead();

    /**
     * Resets the counter for the number of bytes read.
     */
    public abstract void resetBytesRead();

    /**
     * Reads a boolean value from the stream.
     * @return	The boolean read.
     */
    public abstract boolean readBoolean() throws IOException;

    public int readUnsignedShort() throws IOException {
        return readShort() & 0177777;
    }

    public int readUnsignedByte() throws IOException {
        return readByte() & 0377;
    }

    public void readArray(boolean[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(byte[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(char[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(short[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(int[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(long[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(float[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(double[] source) throws IOException {
        readArray(source, 0, source.length);
    }
    
    public abstract int bufferSize();
}
