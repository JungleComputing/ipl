/**
 * Interface to indicate whether the output that implements it
 * supports buffered output. Examples are NetBufferedOutput or
 * NetSplitter.
 */

package ibis.impl.net;

import java.io.IOException;

public interface NetBufferedOutputSupport {

    /**
     * @return whether buffered writing is actually supported
     */
    public boolean writeBufferedSupported();

    /**
     * Sends the current buffer over the network.
     *
     * @throws IOException on error or if buffered writing is not supported
     */
    public void flushBuffer() throws IOException;

    /**
     * Writes <code>length</code> bytes to the underlying output.
     *
     * @param data byte array that is filled
     * @param offset offset in <code>data</code>
     * @param length number of bytes to write.
     *
     * @throws IOException on error or if buffered writing is not supported
     */
    public void writeBuffered(byte[] data, int offset, int length)
	    throws IOException;

}

