package ibis.ipl.impl.net.rel;

import ibis.ipl.impl.net.NetAllocator;
import ibis.ipl.impl.net.NetReceiveBuffer;

class RelReceiveBuffer extends NetReceiveBuffer {

    /**
     * {@inheritDoc}
     */
    RelReceiveBuffer(byte[] data, int length) {
	super(data, length);
    }

    /**
     * {@inheritDoc}
     */
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
