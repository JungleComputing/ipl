/* $Id: BlockingChannelNioDissipator.java 2944 2005-03-15 17:00:32Z ndrost $ */

package ibis.impl.nio;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;

/**
 * Dissipator which reads from a single channel, with the channel normally in
 * blocking mode.
 */
final class BlockingChannelNioDissipator extends NioDissipator implements
        Config {

    BlockingChannelNioDissipator(ReadableByteChannel channel)
            throws IOException {
        super(channel);

        if (!(channel instanceof SelectableChannel)) {
            throw new Error("wrong type of channel given on creation of"
                    + " BlockingChannelNioDissipator");
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
}
