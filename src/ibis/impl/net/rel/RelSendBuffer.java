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
	if (RelConstants.DEBUG) {
	    fragCount = -1;
	}
	reset(false);
    }

    /**
     * {@inheritDoc}
     */
    RelSendBuffer(byte[] data, int length, NetAllocator allocator) {
	super(data, length, allocator);
	if (RelConstants.DEBUG) {
	    fragCount = -1;
	}
	reset(false);
    }


    /**
     * @{inheritDoc}
     */
    public void reset() {
	reset(true);
    }

    private void reset(boolean appReset) {
	sent = false;
	acked = false;
	lastSent = -1;
	ownershipClaimed = false;
	if (RelConstants.DEBUG && appReset) {
	    System.err.println("............................. Clear packet " + fragCount);
	}
    }

}
