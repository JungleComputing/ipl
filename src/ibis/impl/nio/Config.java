package ibis.impl.nio;

/**
 * General configuration file for NioIbis.
 */
interface Config {


    // Debug levels supported in NioIbis

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


    //*** START OF CONFIGURABLE OPTIONS ***\\

    /*
     * Current Debug level.
     */
    static final int DEBUG_LEVEL = LOW_DEBUG_LEVEL;

    /*
     * Do we we asserts (make make it a bit slower)
     */
    static final boolean ASSERT = true;

    /**
     * Buffer sized used in ibis serialization. Must be a multiple of eight.
     */
    static final int BUFFER_SIZE = 1048;

}
