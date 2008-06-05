package ibis.ipl.impl.mx;

/**
 * General configuration file for MxIbis.
 */
// taken from NioIbis
interface Config {

    /** Byte buffer size used. Must be a multiple of eight.  */
    static final int BYTE_BUFFER_SIZE = 60 * 1024;

    /**
     * Buffer sized used for primitive buffers. Must be a multiple of eight.
     */
    static final int PRIMITIVE_BUFFER_SIZE = 6 * 1024;
    
}