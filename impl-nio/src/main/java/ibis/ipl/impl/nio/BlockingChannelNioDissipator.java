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

/**
 * Dissipator which reads from a single channel, with the channel normally in
 * blocking mode.
 */
final class BlockingChannelNioDissipator extends NioDissipator {

    BlockingChannelNioDissipator(ReadableByteChannel channel)
            throws IOException {
        super(channel);

        if (!(channel instanceof SelectableChannel)) {
            throw new IOException("wrong type of channel given on creation of"
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
