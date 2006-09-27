/* $Id$ */

package ibis.impl.nio;

/**
 * General configuration file for NioIbis.
 */
interface Config {

    /*
     * Do we do asserts (may make it a bit slower)
     */
    static final boolean ASSERT = false;

    /*
     * Gather and print some timing statistics
     */
    static final boolean STATS = false;

    /**
     * Byte buffer size used. Must be a multiple of eight.
     */
    static final int BYTE_BUFFER_SIZE = 60 * 1024;

    /**
     * Buffer sized used for primitive buffers. Must be a multiple of eight.
     */
    static final int PRIMITIVE_BUFFER_SIZE = 6 * 1024;

}
