package ibis.impl.nio;

/**
 * General configuration file for NioIbis.
 */
interface Config {

    /**
     * Enables debug support in nio ibis. Use the ibis.nio.debug system
     * property to select which debug statements to print. See Debug.java
     */
    static final boolean DEBUG = true;

    /*
     * Do we do asserts (may make it a bit slower)
     */
    static final boolean ASSERT = true;

    /**
     * Byte buffer size used. Must be a multiple of eight.
     */
    static final int BYTE_BUFFER_SIZE = 60 * 1024;

    /**
     * Buffer sized used for primitive buffers. 
     * Must be a multiple of eight.
     */
    static final int PRIMITIVE_BUFFER_SIZE = 1400;

}
