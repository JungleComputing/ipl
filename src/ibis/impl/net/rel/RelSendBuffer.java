package ibis.ipl.impl.net.rel;

import ibis.ipl.IbisIOException;

import ibis.ipl.impl.net.NetAllocator;
import ibis.ipl.impl.net.NetSendBuffer;

class RelSendBuffer extends NetSendBuffer {

    /**
     * Linked list
     */
    RelSendBuffer next;

    /**
     * Sequence number of this packet
     */
    int		fragCount;

    /**
     * Keep track whether we already sent this packet
     */
    boolean	sent;

    /**
     * And remember when it was: don't do a duplicate too soon
     */
    long	lastSent;

    /**
     * Keep track whether the packet has already been acked
     */
    boolean	acked;


    /**
     * {@inheritDoc}
     */
    RelSendBuffer(byte[] data, int length) {
	super(data, length);
	reset();
    }

    /**
     * {@inheritDoc}
     */
    RelSendBuffer(byte[] data, int length, NetAllocator allocator) {
	super(data, length, allocator);
	reset();
    }


    /**
     * @{inheritDoc}
     */
    public void reset() {
	sent = false;
	acked = false;
	lastSent = -1;
	ownershipClaimed = false;
	if (RelConstants.DEBUG) {
	    System.err.println("............................. Free packet " + fragCount);
	}
    }

}
