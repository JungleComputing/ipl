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
import java.io.OutputStream;

/**
 * A general data output stream. Provides methods to write data to an underlying
 * implementation. Data written to the stream may NOT be touched until after the
 * {@link #flush()} method is called, and the {@link #finished()} method has
 * indicated that the data may be touched again. The write methods from
 * <code>java.io.OutputStream</code> are an exception to this rule: arrays
 * written using these methods can be touched again immediately, so that no
 * extra demands are required when regarding a <code>DataOutputStream</code> as
 * a <code>java.io.OutputStream</code>.
 */
public abstract class DataOutputStream extends OutputStream
        implements DataOutput {

    /**
     * Returns the number of bytes that was written to the stream, in the stream
     * dependent format. This is the number of bytes that will be sent over the
     * network.
     * 
     * @return the number of bytes
     */
    public abstract long bytesWritten();

    /**
     * Resets the counter for the number of bytes written.
     */
    public abstract void resetBytesWritten();

    /**
     * Checks whether all data has been written after a flush.
     * 
     * @return true if all data has been written after a flush.
     * @throws IOException
     *             on I/O error
     */
    public boolean finished() throws IOException {
        // Default implementation returns true.
        return true;
    }

    @Override
    public abstract void flush() throws IOException;

    @Override
    public abstract void close() throws IOException;

    /**
     * Blocks until the data is written.
     * 
     * @exception IOException
     *                on IO error.
     */
    public void finish() throws IOException {
        // Default implementation does nothing.
    }

    public void writeArray(boolean[] source) throws IOException {
        writeArray(source, 0, source.length);
    }

    public void writeArray(byte[] source) throws IOException {
        writeArray(source, 0, source.length);
    }

    public void writeArray(char[] source) throws IOException {
        writeArray(source, 0, source.length);
    }

    public void writeArray(short[] source) throws IOException {
        writeArray(source, 0, source.length);
    }

    public void writeArray(int[] source) throws IOException {
        writeArray(source, 0, source.length);
    }

    public void writeArray(long[] source) throws IOException {
        writeArray(source, 0, source.length);
    }

    public void writeArray(float[] source) throws IOException {
        writeArray(source, 0, source.length);
    }

    public void writeArray(double[] source) throws IOException {
        writeArray(source, 0, source.length);
    }

    abstract public int bufferSize();
}
