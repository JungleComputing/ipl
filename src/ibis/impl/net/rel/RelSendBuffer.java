package ibis.impl.net.rel;

import ibis.impl.net.NetAllocator;
import ibis.impl.net.NetSendBuffer;

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


    RelSendBuffer(byte[] data, int length) {
	super(data, length);
	if (RelConstants.DEBUG) {
	    fragCount = -1;
	}
	reset(false);
    }

    RelSendBuffer(byte[] data, int length, NetAllocator allocator) {
	super(data, length, allocator);
	if (RelConstants.DEBUG) {
	    fragCount = -1;
	}
	reset(false);
    }


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
