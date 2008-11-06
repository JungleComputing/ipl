package ibis.ipl.impl.mx.channels;

interface Config {
	static final int STREAMBUFSIZE = 32 * 1024;
	static final int BUFSIZE = 16 * 1024;
	static final int BUFFERS = 2;
	
	/**
     * Maximum number of buffers in the flush queue at the ScatteringOutputStream 
     */
    static final int FLUSH_QUEUE_SIZE = 2;
}
