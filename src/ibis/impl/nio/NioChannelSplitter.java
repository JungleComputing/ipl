package ibis.impl.nio;

import java.util.ArrayList;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;

import ibis.ipl.IbisError;

/**
 * Writes Buffers given to it to one or more output channels.
 */
public final class NioChannelSplitter implements GatheringByteChannel{

    private static final class Connection {
	GatheringByteChannel channel;
	Selector selector = null;

	Connection(GatheringByteChannel channel) {
	    this.channel = channel;
	}

	/**
	 * returns the selector for this channel. Creates a new selector if
	 * it doesn't exist.
	 */
	private Selector selector() throws IOException {
	    if(selector == null) {
		selector = Selector.open();

		((SelectableChannel) channel).register(selector, 
							SelectionKey.OP_WRITE);
	    }
	    return selector;
	}
    }


    private Connection[] connections; 
    private int used = 0; // connections in use

    private long count = 0; // bytes given to splitter

    NioChannelSplitter() {
	connections = new Connection[8];
    }

    /**
     * No way to close a splitter.
     */
    public boolean isOpen() {
	return true;
    }

    /**
     * No way to close a splitter.
     */
    public void close() {
	//IGNORE
    }

    /**
     * returns the number of bytes written to channels by this splitter
     */
    public long getCount() {
	return count;
    }

    /**
     * resets the byte count to zero
     */
    public void resetCount() {
	count = 0;
    }

    /**
     * adds a channel to this splitters channels. Will write all data written
     * with write(...) to this channel from now on
     */
    public void add(GatheringByteChannel channel) {
	if (connections.length == used) {
	    Connection[] newConnections = new Connection[used * 2];
	    for (int i = 0; i < used; i++) {
		newConnections[i] = connections[i];
	    }
	    connections = newConnections;
	}
	connections[used] = new Connection(channel);
	used += 1;
    }

    /**
     * removes the given channel from the channels this splitter
     * writes to
     */
    public void remove(GatheringByteChannel channel) throws IOException {

	for (int i = 0; i < used; i++) {
	    if ( connections[i].channel == channel) {
		connections[i] = connections[used];
		connections[used] = null;
		used -= 1;
		return;
	    }
	}

	throw new IOException("tried to remove unknown channel from splitter");
    }

    /** returns if this splitter has any channels
     */
    public boolean isEmpty() {
	return (used == 0);
    }

    /**
     * Writes the content of the buffer to all the channels of this splitter.
     * WARNING: Changes the "mark" of the buffer
     */
    public int write(ByteBuffer src) throws NioSplitterException {
	// the number of bytes we'll write to each channel
	int count = src.remaining(); 
	NioSplitterException se = null;

	src.mark(); // remember the current position;


	for(int i = 0; i < used; i++) {

	    src.reset(); // go back to the marked position
	    try {
		connections[i].channel.write(src);
		while(src.hasRemaining()) {
		    //do a select so we are sure we can write at least one
		    //byte more into the channel
		    connections[i].selector().select();

		    connections[i].channel.write(src);
		}
		    
	    } catch (IOException e) {
		// add exceptions to the list, and throw when we're done
		if(se == null) {
		    se = new NioSplitterException();
		}
		se.add(connections[i].channel, e);

		// remove channel from out output set
		try {
		    remove(connections[i].channel);
		} catch (IOException e2) {
		    throw new IbisError("Splitter: removing of known-to-exist channel failed");
		}
		i--; // go back one
	    }
	}

	//FIXME : do we count if an exception is thrown?
	this.count += count;

	if(se != null) {
	    throw se;
	}

	return count;
    }

    /**
     * Writes the content of the buffers to all the channels of this splitter.
     * WARNING: Changes the "mark" of the buffers
     */
    public long write(ByteBuffer[] srcs) throws NioSplitterException{
	return write(srcs, 0, srcs.length);
    }


    /**
     * Writes the content of the buffers from offset to offset+length
     * to all the channels of this splitter.
     * WARNING: Changes the "mark" of the buffers
     */
    public long write(ByteBuffer[] srcs, int off, int len) 
						throws NioSplitterException {
	long count = 0;
	NioSplitterException se = null;
	ByteBuffer lastBuffer = srcs[off];

	for(int i = off; i < (off + len); i++) {
	    srcs[i].mark(); // remember the current position;
	    if(srcs[i].hasRemaining()) {
		lastBuffer = srcs[i];
	    }
	}

	for(int i = 0; i < used; i++) {
	    int tmpCount = 0;
	    for(int j = off; j < (off + len); j++) {
		srcs[j].reset(); // go back to the marked position
	    }
	    try {
		tmpCount += connections[i].channel.write(srcs, off, len);

		while(lastBuffer.hasRemaining()) {
		    //do a select so we can write at least one byte more
		    connections[i].selector().select();
		    tmpCount += connections[i].channel.write(srcs, off, len);
		}

		// sending succeedded, remember how much we send
		count = tmpCount;

	    } catch (IOException e) {
		// add exceptions to the list, and throw when we're done
		if(se == null) {
		    se = new NioSplitterException();
		}
		se.add(connections[i].channel, e);

		// remove channel from out output set
		try {
		    remove(connections[i].channel);
		} catch (IOException e2) {
		    throw new IbisError("Splitter: removing of known-to-exist channel failed");
		}
		i--; // go back one
	    }
	}

	//FIXME : do we count if an exception is thrown?
	this.count += count;

	if(se != null) {
	    throw se;
	}

	return count;
    }
}
