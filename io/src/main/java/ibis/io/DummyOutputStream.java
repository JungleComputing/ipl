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
 * An <code>OutputStream</code> that can be placed on top of any existing
 * <code>java.io.OutputStream</code>. It adds statistics and prevents a
 * <code>close</code> from propagating to the streams below. You need to use
 * {@link #realClose()} for that.
 */
public class DummyOutputStream extends OutputStream {

    private static final boolean SUPPORT_STATS = true;

    private OutputStream out;

    private long count = 0;

    public DummyOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);

        if (SUPPORT_STATS) {
            count++;
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);

        if (SUPPORT_STATS) {
            count += b.length;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);

        if (SUPPORT_STATS) {
            count += len;
        }
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Dummy close to prevent propagating the close to the underlying streams.
     */
    @Override
    public void close() {
        /*
         * Don't propagate the close, otherwise we close the underlying socket, and that
         * is not what we want here.
         */
    }

    /**
     * Closes the underlying streams as well.
     *
     * @exception IOException gets thrown when an IO error occurs.
     */
    public void realClose() throws IOException {
        out.close();
    }

    /**
     * Resets the "number of bytes written" counter.
     */
    public void resetCount() {
        count = 0;
    }

    /**
     * Returns the number of bytes written to this stream since the last call to
     * {@link #resetCount} or the beginning of its existence.
     *
     * @return the number of bytes written since the last reset.
     */
    public long getCount() {
        return count;
    }
}
