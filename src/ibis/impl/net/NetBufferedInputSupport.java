/* $Id$ */

package ibis.impl.net;

import java.io.IOException;

/**
 * Interface to indicate whether an input allows reading
 * <strong>less</strong> than the specified data size.
 * Examples are {@link ibis.impl.net.NetBufferedInput} or {@link ibis.impl.net.NetPoller}.
 */

public interface NetBufferedInputSupport {

    /**
     * Indicate whether the class thats implement this interface actually
     * implement buffered input.
     *
     * @return whether buffered reading is actually supported
     */
    public boolean readBufferedSupported();

    /**
     * Reads up to <code>length</code> bytes from the underlying input.
     *
     * @param data byte array that is filled
     * @param offset offset in <code>data</code>
     * @param length number of bytes to read.
     *
     * @return the number of bytes actually read, or -1 in the case
     *         of end-of-file
     * @throws IOException on error or if buffered reading is not supported
     */
    public int readBuffered(byte[] data, int offset, int length)
            throws IOException;

}
