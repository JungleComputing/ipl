package ibis.ipl.impl.net.rel;

import ibis.ipl.impl.net.NetConvert;

/**
 * The NetIbis shared constants
 */
public interface RelConstants {

    final static boolean	DEBUG = false; // true;
    final static boolean	DEBUG_REXMIT = DEBUG;
    final static boolean	DEBUG_ACK = DEBUG;
    final static boolean	DEBUG_PIGGY = DEBUG;

    final static int		FIRST_PACKET_COUNT = 0;
    final static int		LAST_FRAG_BIT	= (0x1 << 31);

    final static boolean	USE_EXPLICIT_ACKS = true;
    final static boolean	USE_PIGGYBACK_ACKS = true;

    /**
     * Data packet layout:
     *  . fragCount		:: integer	seqno of this fragment
     *  . partnerIndex		:: integer
     * [ piggy ack data: ]
     *  . piggy data control packet
     *
     * Control packet layout:
     *  . nextContiguous	::
     *  . nextContiguous	:: integer	contiguously r'cved up to here
     *  . scatteredRecv		:: integer[ACK_SET_IN_INTS]
     *  					non-contig r'cved, offset from
     *  					nextContiguous
     */

    final static int	ACK_SET_SIZE = 64;

    final static int	ACK_SET_IN_INTS = (ACK_SET_SIZE + NetConvert.BITS_PER_INT - 1) / NetConvert.BITS_PER_INT;

    // The length of the ack header expressed in bytes
    // Here, we don't count the frag count
    final static int	headerLength =
				NetConvert.INT_SIZE	// the index for the reverse connection
				+ NetConvert.INT_SIZE	// the contiguous ack
				+ NetConvert.INT_SIZE * ACK_SET_IN_INTS; // ack bitset beyond contiguous

    /**
     * Sweeper interval in ms.
     */
    static final long	sweepInterval = 1000;

    final static long	SHUTDOWN_DELAY = 0 * sweepInterval;	// Wait for partner's last ack


    /**
     * For debugging, throw away arbitrary packets.
     * The (double) number this is set to indicates the chance of a discard
     */
    final static double RANDOM_DISCARDS = 0.0; // 0.2; // 0.5;

}
