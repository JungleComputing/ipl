package ibis.impl.nio;

import java.util.ArrayList;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;

import ibis.ipl.IbisError;

/**
 * Writes Buffers given to it to one or more output channels.
 */
public final class NioChannelSplitter implements GatheringByteChannel{

    private GatheringByteChannel[] channels;
    private int used = 0; // channels in use

    private long count = 0; // bytes given to splitter

    NioChannelSplitter() {
	channels = new GatheringByteChannel[8];
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
	if (channels.length == used) {
	    GatheringByteChannel[] newChannels = new GatheringByteChannel[used * 2];
	    for (int i = 0; i < used; i++) {
		newChannels[i] = channels[i];
	    }
	    channels = newChannels;
	}
	channels[used] = channel;
	used += 1;
    }

    /**
     * removes the given channel from the channels this splitter
     * writes to
     */
    public void remove(GatheringByteChannel channel) throws IOException {

	for (int i = 0; i < used; i++) {
	    if ( channels[i] == channel) {
		channels[i] = channels[used];
		channels[used] = null;
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
	int count = src.remaining(); // the number of bytes we'll write to each channel
	NioSplitterException se = null;

	src.mark(); // remember the current position;


	for(int i = 0; i < used; i++) {
	    src.reset(); // go back to the marked position
	    try {
		do {
		    channels[i].write(src);
		} while(src.hasRemaining());
	    } catch (IOException e) {
		// add exceptions to the list, and throw when we're done
		if(se == null) {
		    se = new NioSplitterException();
		}
		se.add(channels[i], e);

		// remove channel from out output set
		try {
		    remove(channels[i]);
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
		do {
		    tmpCount += channels[i].write(srcs, off, len);
		} while(lastBuffer.hasRemaining());
		count = tmpCount; // sending succeedded, remember how much we send
	    } catch (IOException e) {
		// add exceptions to the list, and throw when we're done
		if(se == null) {
		    se = new NioSplitterException();
		}
		se.add(channels[i], e);

		// remove channel from out output set
		try {
		    remove(channels[i]);
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
