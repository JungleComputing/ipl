package ibis.impl.nio;

import java.net.InetSocketAddress;

import java.nio.channels.Channel;

import java.io.IOException;

/**
 * creates and recycles/destroys writeable and readable channels. Any
 * implementation should also handle incoming connections in the run() 
 * method and register them with the receiveport.
 */
public interface NioChannelFactory extends Runnable, Config {

    /**
     * Tries to connect to sendport to the given receiveport 
     * for tileoutMillis milliseconds and returns the writechannel
     * if it succeeded. a timeout of 0 means try forever.
     *
     * @return a new WritableChannel connected to "rpi". May also be a
     * GatheringByteChannel
     */
    public Channel connect(NioSendPortIdentifier spi,
	    NioReceivePortIdentifier rpi,
	    long timeoutMillis) throws IOException;

    /**
     * Returns a write channel to the factory for recycling or destruction.
     */
    public void recycle(NioReceivePortIdentifier rpi, 
	    Channel channel) throws IOException;

    /**
     * Returns a read channel to the factory for recycling or destruction.
     * Will save the given attachment and give it to the receiveport if it's
     * connected again (usefull for keeping buffers)
     */
    public void recycle(NioSendPortIdentifier spi, 
	    Channel channel) throws IOException;

    /**
     * Register a receiveport with this factory, so it will listen for
     * connections to it from now on.
     *
     * @return the address of the socket we wil listen for connections on
     */
    public InetSocketAddress register(NioReceivePort rp) throws IOException;

    /**
     * De-register a receiveport with the factory
     */
    public void deRegister(NioReceivePort rp) throws IOException;

    /**
     * stops the factory. It will kill off any threads it made
     */
    public void quit() throws IOException;
}
