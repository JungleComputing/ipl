package ibis.impl.net.rel;

import ibis.impl.net.NetAllocator;
import ibis.impl.net.NetReceiveBuffer;

class RelReceiveBuffer extends NetReceiveBuffer {

    RelReceiveBuffer(byte[] data, int length) {
	super(data, length);
    }

    RelReceiveBuffer(byte[] data, int length, NetAllocator allocator) {
	super(data, length, allocator);
    }

    /**
     * Linked list
     */
    RelReceiveBuffer next;

    /**
     * Sequence number of this packet
     */
    int		fragCount;

    /**
     * Is this the last fragment of a message?
     */
    boolean	isLastFrag;

}
