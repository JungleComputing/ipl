/**
 * Interface to indicate whether the input that implements it
 * supports buffered input. Examples are NetBufferedInput or
 * NetPoller.
 */

package ibis.impl.net;

import java.io.IOException;

public interface NetBufferedInputSupport {

    /**
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
