/* $Id$ */

package ibis.impl.nio;

import ibis.ipl.IbisError;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.apache.log4j.Logger;

/**
 * Dissipator which reads from a single channel, with the channel normally in
 * non-blocking mode.
 */
final class NonBlockingChannelNioDissipator extends NioDissipator implements
        Config {

    static Logger logger = Logger
            .getLogger(NonBlockingChannelNioDissipator.class.getName());

    Selector selector;

    NonBlockingChannelNioDissipator(NioSendPortIdentifier peer,
            NioReceivePortIdentifier rpi, ReadableByteChannel channel,
            NioPortType type) throws IOException {
        super(peer, rpi, channel, type);

        if (!(channel instanceof SelectableChannel)) {
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
        if (logger.isDebugEnabled()) {
            logger.info("filling buffer");
        }

        // Always do one read, even if it isn't strictly needed
        // and without looking if we're going to get any data.
        readFromChannel();

        while (unUsedLength() < minimum) {
            if (logger.isDebugEnabled()) {
                logger.info("doing a select for data");
            }
            selector.select();
            selector.selectedKeys().clear();
            readFromChannel();
        }

        if (logger.isDebugEnabled()) {
            logger.info("filled buffer");
        }
    }

    public void reallyClose() throws IOException {
        selector.close();
        channel.close();
    }
}