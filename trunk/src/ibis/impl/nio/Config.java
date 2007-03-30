/* $Id: Config.java 2944 2005-03-15 17:00:32Z ndrost $ */

package ibis.impl.nio;

/**
 * General configuration file for NioIbis.
 */
interface Config {

    /** Byte buffer size used. Must be a multiple of eight.  */
    static final int BYTE_BUFFER_SIZE = 60 * 1024;

    /**
     * Buffer sized used for primitive buffers. Must be a multiple of eight.
     */
    static final int PRIMITIVE_BUFFER_SIZE = 6 * 1024;
}
