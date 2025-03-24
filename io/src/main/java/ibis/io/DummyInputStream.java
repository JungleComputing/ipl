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
import java.io.InputStream;

/**
 * An <code>InputStream</code> that can be placed on top of any existing
 * <code>java.io.InputStream</code>. It adds statistics and prevents a
 * <code>close</code> from propagating to the streams below. You need to use
 * {@link #realClose()} for that.
 */

public class DummyInputStream extends InputStream {

    static final boolean SUPPORT_STATS = true;

    InputStream in;

    long count = 0;

    public DummyInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        // System.err.println("dummy.read");
        if (SUPPORT_STATS) {
            count++;
        }
        return in.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        int res = in.read(b);
        // System.err.println("dummy.read array of len " + b.length
        // + " result was " + res + " bytes");
        if (SUPPORT_STATS) {
            if (res >= 0) {
                count += res;
            }
        }
        return res;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int res = in.read(b, off, len);
        // System.err.println("dummy.read array of len " + len
        // + " result was " + res + " bytes");
        if (SUPPORT_STATS) {
            if (res >= 0) {
                count += res;
            }
        }
        return res;
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    /**
     * Dummy close to prevent propagating the close to the underlying streams.
     */
    @Override
    public void close() {
        /* ignore */
    }

    /**
     * Closes the underlying streams as well.
     *
     * @exception IOException gets thrown when an IO error occurs.
     */
    public void realClose() throws IOException {
        in.close();
    }

    @Override
    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    /**
     * Resets the "number of bytes read" counter.
     */
    public void resetCount() {
        count = 0;
    }

    /**
     * Returns the number of bytes read from this stream since the last call to
     * {@link #resetCount} or the beginning of its existence.
     *
     * @return the number of bytes read since the last reset.
     */
    public long getCount() {
        return count;
    }
}
