/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.ipl.impl.nio;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dissipator which reads from a single channel, with the channel normally in
 * non-blocking mode.
 */
final class NonBlockingChannelNioDissipator extends NioDissipator {
    private static Logger logger = LoggerFactory.getLogger(NonBlockingChannelNioDissipator.class);

    Selector selector;

    NonBlockingChannelNioDissipator(ReadableByteChannel channel) throws IOException {
        super(channel);

        if (!(channel instanceof SelectableChannel)) {
            throw new IOException("wrong type of channel given on creation of" + " ChannelNioDissipator");
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
    @Override
    protected void fillBuffer(int minimum) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("filling buffer");
        }

        // Always do one read, even if it isn't strictly needed
        // and without looking if we're going to get any data.
        readFromChannel();

        while (unUsedLength() < minimum) {
            if (logger.isDebugEnabled()) {
                logger.debug("doing a select for data");
            }
            selector.select();
            selector.selectedKeys().clear();
            readFromChannel();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("filled buffer");
        }
    }

    @Override
    public void close() throws IOException {
        selector.close();
        super.close();
    }
}
