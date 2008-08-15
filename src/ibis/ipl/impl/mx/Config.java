package ibis.ipl.impl.mx;

/**
 * General configuration file for MxIbis.
 */
// taken from NioIbis
interface Config {

    /**
     * Buffer sized used for primitive buffers. Must be a multiple of eight.
     */
    static final int PRIMITIVE_BUFFER_SIZE = 8 * 1024; 
    //TODO must be smaller than 32768 due to short use in sendbuffer header
    //TODO Try a minimum and maximum size instead of a fixed one to combine low latencies with high throughput
    /**
     * Maximum number of buffers in the flush queue at the MxDataOutputStreams and MxWriteChannels 
     */
    static final int FLUSH_QUEUE_SIZE = 8;
    
}