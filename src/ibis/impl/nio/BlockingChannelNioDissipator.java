package ibis.impl.nio;

import ibis.ipl.IbisError;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;

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

    public void reallyClose() throws IOException {
	channel.close();
    }
}
