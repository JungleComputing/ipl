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
package ibis.ipl.impl.stacking.lrmc.util;

import java.io.IOException;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

public class Message {

    public static final int LAST_PACKET = 1 << 31;

    public int sender;

    public short refcount;

    public int[] destinations;
    public int destinationsUsed;

    public int id;
    public int num;

    public byte[] buffer;
    public int off;
    public int len;

    public boolean last = false;
    public boolean local = false;

    public Message next;

    // private int useCount = 0;

    public Message(int len) {
        buffer = new byte[len];
    }

    public void read(ReadMessage rm, int len, int dst) throws IOException {

        this.off = 0;
        this.len = len;
        this.local = false;

        sender = rm.readInt();
        id = rm.readInt();
        num = rm.readInt();

        last = ((num & LAST_PACKET) != 0);

        if (last) {
            num &= ~LAST_PACKET;
        }

        if (len > 0) {
            rm.readArray(buffer, 0, len);
        }

        if (dst > 0) {
            // TODO optimize!
            if (destinations == null || destinations.length < dst) {
                destinations = new int[dst];
            }

            rm.readArray(destinations, 0, dst);
        }

        destinationsUsed = dst;
    }

    public void write(WriteMessage wm, int fromDest) throws IOException {

        int destinationLength = destinationsUsed - fromDest;

        // First write the two variable lengths present in the message.
        wm.writeInt(len);
        wm.writeInt(destinationLength);

        // Then write the content that guaranteed to be there
        wm.writeInt(sender);
        wm.writeInt(id);

        if (last) {
            wm.writeInt(num | Message.LAST_PACKET);
        } else {
            wm.writeInt(num);
        }

        // Finally write the actual data that has a variable size
        if (len > 0) {
            wm.writeArray(buffer, off, len);
        }

        if (destinationLength > 0) {
            wm.writeArray(destinations, fromDest, destinationLength);
        }
    }
}
