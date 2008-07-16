package ibis.ipl.impl.mx;

/**
 * General configuration file for MxIbis.
 */
// taken from NioIbis
interface Config {

    /** Byte buffer size used. Must be a multiple of eight.  */
//    static final int BYTE_BUFFER_SIZE = 80 * 1024;

    /**
     * Buffer sized used for primitive buffers. Must be a multiple of eight.
     */
    static final int PRIMITIVE_BUFFER_SIZE = 8 * 1024; 
    //TODO must be smaller than 32767 due to short use in sendbuffer header
    
    /**
     * Maximum number of buffers in the flush queue at the MxDataOutputStreams and MxWriteChannels 
     */
    static final int FLUSH_QUEUE_SIZE = 8;
    
}