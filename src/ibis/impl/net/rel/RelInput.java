package ibis.ipl.impl.net.rel;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisIOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * The REL input implementation.
 */
public final class RelInput
    extends NetBufferedInput
    // extends NetInput
    implements RelConstants, RelSweep {

    private final static boolean STATISTICS = Driver.STATISTICS;
    private final static boolean POLL_DOES_ONE_SHOT = true;

    /**
     * My poller flag
     */
    private Integer	spn;

    /**
     * The driver used for the 'real' input.
     */
    private NetDriver subDriver = null;

    private NetBufferFactory subFactory = null;

    /**
     * The communication input.
     */
    private NetInput  dataInput  = null;

    private Driver	relDriver;

    /**
     * The index number of our piggyback partner
     */
    private int		partnerIndex;

    private int		headerStart;
    private int		ackStart;

    /**
     * The acknowledgement output.
     */
    private NetOutput	controlOutput = null;
    private int		controlHeaderStart;


    /**
     * The sliding window descriptor.
     * The window size is in rel.Driver.
     */
    private int	nextDeliver	= FIRST_PACKET_COUNT;
    private int	nextContiguous	= FIRST_PACKET_COUNT;	// From here on, require bitset ack
    private int	waitingForCount	= FIRST_PACKET_COUNT - 1; // Sync between sender and open window
    private int	nextNonMissing	= FIRST_PACKET_COUNT;	// If sent missing bit set, 1+max(set)

    /**
     * Maintain maximum observed fragCount, for ack test shortcut
     */
    private int	maxFragCount = FIRST_PACKET_COUNT - 1;

    /**
     * The sliding window receive queue.
     */
    private RelReceiveBuffer	front;
    private RelReceiveBuffer	tail;

    private int	windowSize;
    private int	lastContiguousAck = FIRST_PACKET_COUNT - 1;

    private boolean	startMsg = true;

    /**
     * Cache ack send and receive bitsets.
     */
    private int[]	ackSendSet = new int[ACK_SET_IN_INTS];


    /**
     * The explicit ack packet. Need only one because we keep
     * the lock during the ack send.
     */
    private NetSendBuffer	ackPacket;


    private int nDuplicates = 0;


    java.util.Random discardRandom;


    static {
	if (! USE_EXPLICIT_ACKS) {
	    System.err.println("Ibis.net.rel: Do not use explicit acks");
	}
    }

    /**
     * Constructor.
     *
     * @param sp the properties of the input's
     * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
     * @param driver the REL driver instance.
     * @param input the controlling input.
     */
    RelInput(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
	super(pt, driver, up, context);

	relDriver = (Driver)driver;

	if (RANDOM_DISCARDS > 0.0) {
	    discardRandom = new java.util.Random();
	}
    }


    /*
     * {@inheritDoc}
     */
    public void setupConnection(Integer rpn,
				ObjectInputStream is,
				ObjectOutputStream os,
				NetServiceListener nls)
	    throws IbisIOException {

	spn = rpn;

	/* Main connection */
	NetInput dataInput = this.dataInput;
	if (dataInput == null) {
	    if (subDriver == null) {
		String subDriverName = getMandatoryProperty("Driver");
		subDriver = driver.getIbis().getDriver(subDriverName);
	    }

	    dataInput = newSubInput(subDriver);
	    this.dataInput = dataInput;
	}

	dataInput.setupConnection(rpn, is, os, nls);

	headerStart = dataInput.getHeadersLength();
System.err.println("RelInput: headerStart " + headerStart);
	ackStart     = headerStart + NetConvert.INT_SIZE;
	dataOffset   = ackStart + RelConstants.headerLength;
	mtu          = dataInput.getMaximumTransfertUnit();

	if (subFactory == null) {
	    if (DEBUG) {
		System.err.println("Select BufferFactory for dataInput " +
				   dataInput);
	    }
	    subFactory = new NetBufferFactory(mtu, new RelReceiveBufferFactoryImpl());
	    dataInput.setBufferFactory(subFactory);
	} else {
	    subFactory.setMaximumTransferUnit(mtu);
	}

	try {
	    os.writeObject(relDriver.getIbis().identifier());
	    IbisIdentifier partnerId = (IbisIdentifier)is.readObject();
	    partnerIndex = is.readInt();
	    windowSize = is.readInt();

	    relDriver.registerInputConnection(this, partnerId);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	} catch (java.lang.ClassNotFoundException e) {
	    throw new IbisIOException(e);
	}

	/* Reverse connection */
	NetOutput controlOutput = this.controlOutput;
	if (controlOutput == null) {
		controlOutput = newSubOutput(subDriver);
		this.controlOutput = controlOutput;
	}

	controlOutput.setupConnection(new Integer(-1), is, os, nls);
	controlHeaderStart = controlOutput.getHeadersLength();
    }


    public int getHeadersLength() {
	return dataOffset;
    }


    // Call this non-synchronized
    private void handlePiggy(byte[] data, int offset)
	    throws IbisIOException {
	int		partnerNo = NetConvert.readInt(data, offset);
	offset += NetConvert.INT_SIZE;

	RelOutput piggyClient = relDriver.lookupPiggyPartner(partnerNo);
	if (piggyClient != null) {
	    /* Here we require the lock of piggyClient.
	     * We must not hold our lock because of deadlock.
	     */
	    piggyClient.handleAck(data, offset);
	}
    }


    /**
     * Fill ack space in this packet.
     * Return whether it is time to send an explicit ack.
     */
    // Call this synchronized
    private boolean fillAck(byte[] data, int offset, boolean always) {
	RelReceiveBuffer scan = front;
	int lastContiguous = nextContiguous;

	// Shortcut for the common case (avoid scanning for each receipt)
	if (! always &&	/* User wants always, OK */
		nextNonMissing >= maxFragCount &&
			    /* Already reported max. missing packet */
		maxFragCount - lastContiguousAck < windowSize / 2
			    /* Half-window not yet reached */
		) {
	    if (DEBUG_ACK) {
		System.err.println("fillAck: bail out because maxFragCount=" +
			maxFragCount + " - lastContiguousAck=" +
			lastContiguousAck + " < windowSize / 2 = " +
			(windowSize / 2) + " nextNonMissing=" + nextNonMissing +
			" nextContiguous=" + nextContiguous);
	    }
	    NetConvert.writeInt(-1, data, offset);
	    return false;
	}

	while (scan != null && nextContiguous > scan.fragCount) {
	    scan = scan.next;
	}

	if (DEBUG_ACK) {
	    System.err.println("fillAck: initially nextContiguous " + nextContiguous);
	}
	while (scan != null && nextContiguous == scan.fragCount) {
	    nextContiguous++;
	    scan = scan.next;
	}
	if (DEBUG_ACK) {
	    System.err.println("fillAck: ******************** update " +
		    "nextContiguous to " + nextContiguous +
		    "; next received packet " +
		    (scan == null ? -1 : scan.fragCount));
	    // Thread.dumpStack();
	}

	if (! always &&	/* User wants always, OK */
		nextNonMissing >= maxFragCount &&
			    /* Already reported max. missing packet */
		nextContiguous - lastContiguousAck < windowSize / 2
			    /* Half-window not yet reached */
		) {
	    if (DEBUG_ACK) {
		System.err.println("fillAck: bail out because nextContiguous=" +
			nextContiguous + " - lastContiguousAck=" +
			lastContiguousAck + " < windowSize / 2 = " +
			(windowSize / 2) + " nextNonMissing=" + nextNonMissing +
			" nextContiguous=" + nextContiguous);
	    }
	    NetConvert.writeInt(-1, data, offset);
	    return false;
	}

	if (nextContiguous >= nextNonMissing) {
	    nextNonMissing = nextContiguous;
	}
	int lastNonMissing = nextNonMissing;

	for (int i = 0; i < ACK_SET_IN_INTS; i++) {
	    ackSendSet[i] = 0;
	}

	while (scan != null) {
	    int x = scan.fragCount - nextContiguous;
	    int off = x / NetConvert.BITS_PER_INT;
	    if (off >= ACK_SET_IN_INTS) {
		// Leave these be
		break;
	    }
	    int bit = x % NetConvert.BITS_PER_INT;
	    ackSendSet[off] |= (0x1 << bit);
	    if (DEBUG_ACK) {
		if (scan.next != null &&
			scan.fragCount + 1 != scan.next.fragCount) {
		    System.err.println("fillAck: guess I have to report" +
			    " missing from " + (scan.fragCount + 1) +
			    " up to " + scan.next.fragCount);
		}
	    }
	    nextNonMissing = scan.fragCount + 1;
	    scan = scan.next;
	}

	if (DEBUG_ACK) {
	    System.err.println("fillAck: Write nextContigous " +
		    nextContiguous + " into ack space; nextNonMissing := " +
		    nextNonMissing);
	}
	NetConvert.writeInt(nextContiguous, data, offset);
	offset += NetConvert.INT_SIZE;
	NetConvert.writeArraySliceInt(ackSendSet, 0, ACK_SET_IN_INTS,
				      data, offset);
	offset += NetConvert.INT_SIZE * ACK_SET_IN_INTS;

	if (always ||
		lastContiguous != nextContiguous ||
		lastNonMissing != nextNonMissing) {
	    lastContiguousAck = nextContiguous;
	    return true;
	}

	return false;	/* No reason to half-window ack */
    }


    /**
     * Send an ack at half-window size
     *
     * @param always if false, send only at half-window size. Else, send
     * irrespective of our position in the window.
     */
    // Call this synchronized
    private void sendExplicitAck(boolean always) throws IbisIOException {
	if (! USE_EXPLICIT_ACKS) {
	    // System.err.println("Do it with piggy-backed acks only");
	    return;
	}

	if (ackPacket == null) {
	    ackPacket = controlOutput.createSendBuffer(controlHeaderStart + RelConstants.headerLength);
	    // I want to recycle this packet immediately
	    ackPacket.ownershipClaimed = true;
	}
	if (fillAck(ackPacket.data, controlHeaderStart, always)) {
	    if (DEBUG_ACK) {
		RelOutput.reportAck(System.err,
				    ">>>>>>>>>>>>>>>>",
				    ackPacket.data,
				    controlHeaderStart);
	    }
	    if (RANDOM_DISCARDS > 0.0 &&
		    discardRandom.nextDouble() < RANDOM_DISCARDS) {
		System.err.println("Discard explicit ack");
		return;
	    }
	    controlOutput.writeByteBuffer(ackPacket);
	}
    }


    private void handleDuplicate(RelReceiveBuffer packet)
	    throws IbisIOException {
	if (DEBUG_REXMIT) {
	    System.err.println(this + ": !!!!!!!!!!!!!!!!! Receive duplicate packet " + packet.fragCount + "; what should I do?");
	}
	packet.free();
	sendExplicitAck(true);
	if (STATISTICS) {
	    nDuplicates++;
	}
    }


    // Call this synchronized
    private void enqueueReceiveBuffer(RelReceiveBuffer packet)
	    throws IbisIOException {
	int fragCount = NetConvert.readInt(packet.data, headerStart);
	packet.isLastFrag = ((fragCount & LAST_FRAG_BIT) != 0);
	if (packet.isLastFrag) {
	    fragCount &= ~LAST_FRAG_BIT;
	}
	packet.fragCount = fragCount;

	if (fragCount > maxFragCount) {
	    maxFragCount = fragCount;
	}
	if (fragCount == nextNonMissing) {
	    nextNonMissing++;
	}

	if (DEBUG_REXMIT) {
	    if (front == null ?
		    nextDeliver != fragCount :
		    tail.fragCount + 1 != fragCount) {
		System.err.println(this +
			": !!!!!!!!!!!!!!!!! Receive out-of-order packet " +
			fragCount + "; expect " +
			(front == null ? nextDeliver : (tail.fragCount + 1)) +
			" nextDeliver " + nextDeliver + " front " +
			(front == null ? -1 : front.fragCount));
	    } else if (DEBUG_ACK) {
		System.err.println("Receive in-order packet " + packet.fragCount);
	    }
	}

	if (fragCount < nextContiguous) {
	    handleDuplicate(packet);
	} else if (front == null) {
	    front = packet;
	    tail  = packet;
	    packet.next = null;
	} else if (fragCount > tail.fragCount) {
	    tail.next = packet;
	    tail = packet;
	    packet.next = null;
	} else {
	    RelReceiveBuffer scan = front;
	    RelReceiveBuffer prev = null;
	    while (scan.fragCount < fragCount) {
		prev = scan;
		scan = scan.next;
	    }
	    if (scan.fragCount == fragCount) {
		// Receive a duplicate.
		handleDuplicate(packet);
	    } else {
		if (prev == null) {
		    front = packet;
		} else {
		    prev.next = packet;
		}
		packet.next = scan;
	    }
	}

	sendExplicitAck(false);
    }


    /* Call this if you know there is a packet, and are willing to wait until
     * it is really there */
    private void pollBlocking() throws IbisIOException {
	RelReceiveBuffer packet = (RelReceiveBuffer)dataInput.readByteBuffer(mtu);
	if (DEBUG || (DEBUG_REXMIT && DEBUG_ACK)) {
	    int first_int = RelOutput.reportPacket(System.err,
						   "Received Packet length " + packet.length,
						   packet.data,
						   0, // headerStart,
					       headerStart / NetConvert.INT_SIZE +
						   4);
	}
	handlePiggy(packet.data, ackStart);

	synchronized (this) {
	    enqueueReceiveBuffer(packet);
	    if (DEBUG) {
		System.err.println("From poll: receive packet " + packet.fragCount + " size " + packet.length);
	    }
	    handleReceiveContinuations();
	}
    }


    /**
     * {@inheritDoc}
     */
    // Call this non-synchronized
    public Integer poll() throws IbisIOException {
	if (dataInput == null) {
	    return null;
	}

	boolean one_shot = true;
	while (true) {
	    Integer r = null;
	    if (one_shot) {
		r = dataInput.poll();
		if (POLL_DOES_ONE_SHOT) {
		    one_shot = false;
		}
	    }
	    if (r == null) {
// System.err.println("Poll: return null");
// System.err.println("Poll: front.fragCount " + (front == null ? -1 : front.fragCount) + " nextDeliver " + nextDeliver);
		if (front != null && front.fragCount == nextDeliver) {
		    activeNum = spn;
		    if (DEBUG) {
			System.err.println("Poll: return my activeNum " + activeNum);
			// Thread.dumpStack();
		    }
		    return activeNum;
		} else {
		    if (false && DEBUG) {
			System.err.print("_");
		    }
		    return null;
		}
	    } else {
		// mtu          = dataInput.getMaximumTransfertUnit();
		// headerStart  = dataInput.getHeadersLength();
		if (DEBUG) {
		    System.err.println("Initialize " + this + " from poll");
		}
		initReceive();
	    }

	    pollBlocking();
	}
    }


    /* Wait here until the next packet has arrived. */
    // Call this synchronized
    private RelReceiveBuffer dequeueReceiveBuffer() throws IbisIOException {
	while (front == null || front.fragCount != nextDeliver) {
	    pollBlocking();
	    // poll();
	    if (false && (front == null || front.fragCount != nextDeliver)) {
		try {
		    waitingForCount = nextDeliver;
		    wait();
		    waitingForCount = FIRST_PACKET_COUNT - 1;
		} catch (InterruptedException e) {
		    // Ignore
		}
	    }
	}
	nextDeliver++;
	if (nextDeliver > nextContiguous) {
	    nextContiguous = nextDeliver;
	}

	RelReceiveBuffer packet = front;
	front = front.next;

	startMsg = packet.isLastFrag;

	return packet;
    }


    // Call this synchronized
    private void handleReceiveContinuations() {
	if (waitingForCount == nextDeliver) {
	    notifyAll();
	}
    }


    /**
     * {@inheritDoc}
     */
    synchronized
    public NetReceiveBuffer receiveByteBuffer(int length)
	    throws IbisIOException {
	if (DEBUG) {
	    System.err.println("Receive deliver request size " + length);
	    Thread.dumpStack();
	}

	RelReceiveBuffer packet = dequeueReceiveBuffer();
	if (DEBUG) {
	    System.err.println("Deliver packet " + packet.fragCount +
		    " size " + packet.length + "; maxFragSeen " +
		    maxFragCount + " queue front " +
		    (front == null ? -1 : front.fragCount));
	}
	return packet;
    }


    synchronized
    void pushAck(byte[] data, int offset) {
	if (DEBUG_ACK) {
	    System.err.println("Push ack index " + partnerIndex + " offset " + offset);
	}
	NetConvert.writeInt(partnerIndex, data, offset);
	if (DEBUG_ACK) {
	    System.err.println("Push ack data at offset " + (offset + NetConvert.INT_SIZE));
	}
	fillAck(data, offset + NetConvert.INT_SIZE, true);
    }


    public void invoke() {
    }


    void report() {
	if (STATISTICS) {
	    System.err.println(this + ": packets " + nextDeliver + " duplicates " + nDuplicates);
	}
    }


    /**
     * {@inheritDoc}
     */
    public void finish() throws IbisIOException {
	    super.finish();
	    dataInput.finish();
    }


    /**
     * {@inheritDoc}
     */
    public void free() throws IbisIOException {
	Thread.dumpStack();

	for (int i = 0; i < SHUTDOWN_DELAY / sweepInterval; i++) {
	    synchronized (this) {
		sendExplicitAck(true);
		try {
		    wait(sweepInterval);
		} catch (InterruptedException e) {
		    /* Quits if they go interrupting us */
		}
	    }
	}

	report();
	if (dataInput != null) {
	    dataInput.free();
	    dataInput = null;
	}
	if (controlOutput != null) {
	    controlOutput.free();
	    controlOutput = null;
	}

	subDriver = null;
	super.free();
    }

}
