package ibis.impl.nio;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SelectableChannel;
import java.nio.ByteBuffer;

import java.io.IOException;

import ibis.ipl.IbisError;
import ibis.ipl.ReceiveTimedOutException;

/**
 * Dissipator which reads from a single channel, with the channel normally
 * in non-blocking mode.
 */
final class NonBlockingChannelNioDissipator extends NioDissipator 
							    implements Config {

    Selector selector;

    NonBlockingChannelNioDissipator(NioSendPortIdentifier peer,
				    NioReceivePortIdentifier rpi,
				    ReadableByteChannel channel,
				    NioPortType type) throws IOException {
	super(peer, rpi, channel, type);

	if(!(channel instanceof SelectableChannel)) {
	    throw new IbisError("wrong type of channel given on creation of"
		    + " ChannelNioDissipator");
	}

	selector = Selector.open();
	SelectableChannel sc = (SelectableChannel) this.channel;
	sc.configureBlocking(false);
	sc.register(selector, SelectionKey.OP_READ);
    }

    /**
     * fills the buffer upto at least "minimum" bytes.
     *
     */
    protected void fillBuffer(int minimum) throws IOException {
	//Always do one read, even if it isn't strictly needed
	//and without looking if we're going to get any data.
	readFromChannel();

	while (unUsedLength() < minimum) {
	    selector.select();
	    selector.selectedKeys().clear();
	    readFromChannel();
	}
    }

    public void close() throws IOException {
	selector.close();
	channel.close();
    }
}
