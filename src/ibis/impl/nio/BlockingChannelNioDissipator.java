package ibis.impl.nio;

import java.io.IOException;

import java.nio.Buffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;

import ibis.ipl.IbisError;
import ibis.ipl.ReceiveTimedOutException;

/**
 * Dissipator which reads from a single channel, with the channel normally
 * in blocking mode.
 */
final class BlockingChannelNioDissipator extends NioDissipator 
							    implements Config {

    BlockingChannelNioDissipator(NioSendPortIdentifier peer,
				    NioReceivePortIdentifier rpi,
				    ReadableByteChannel channel,
				    NioPortType type) throws IOException {
            super(peer, rpi, channel, type);

	if(!(channel instanceof SelectableChannel)) {
	    throw new IbisError("wrong type of channel given on creation of"
		    + " ChannelNioDissipator");
	}
    }

    /**
     * fills the buffer upto at least "minimum" bytes.
     *
     */
    protected void fillBuffer(int minimum) throws IOException {
	while (unUsedLength() < minimum) {
	    readFromChannel();
	}
    }

    public void close() throws IOException {
	channel.close();
    }
}
