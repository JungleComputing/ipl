package ibis.impl.nio;

/**
 * General configuration file for NioIbis.
 */
interface Config {

    /**
     * Enables debug support in nio ibis. Use the ibis.nio.debug system
     * property to select which debug statements to print. See Debug.java
     */
    static final boolean DEBUG = false;


    //DEBUG LEVELS (deprecated)

    /**
     * Debug level which turns debugging support OFF
     */
    static final int NO_DEBUG_LEVEL = 0;

    /**
     * Debug level which only prints errors.
     */
    static final int ERROR_DEBUG_LEVEL = 1;

    /**
     * Debug level which prints a few numbers here and there (ip address, etc).
     */
    static final int LOW_DEBUG_LEVEL = 2;

    /**
     * "Normal" Debug level
     */
    static final int MEDIUM_DEBUG_LEVEL = 3;

    /**
     * Debug level wich prints a message for every new message and such.
     */
    static final int HIGH_DEBUG_LEVEL = 4;

    /**
     * Debug level which prints a message on every call to every stream.
     *
     */
    static final int RIDICULOUSLY_HIGH_DEBUG_LEVEL = 5;

    static final int DEBUG_LEVEL = ERROR_DEBUG_LEVEL;

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
