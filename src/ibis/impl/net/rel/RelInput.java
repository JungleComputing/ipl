package ibis.ipl.impl.net.rel;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIdentifier;

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
    private Integer		spn;

    /**
     * The driver used for the 'real' input.
     */
    private NetDriver		subDriver = null;

    private NetBufferFactory	subFactory = null;

    /**
     * The communication input.
     */
    private NetInput		dataInput  = null;

    private Driver		relDriver;

    /**
     * The index number of our piggyback partner
     */
    private int			partnerIndex;

    private int			headerStart;
    private int			ackStart;

    /**
     * The acknowledgement output.
     */
    private NetOutput		controlOutput = null;
    private int			controlHeaderStart;
    private NetBufferFactory	controlFactory;


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


    private void checkLocked() throws NetIbisException {
	if (DEBUG_LOCK) {
	    try {
		notify();
		return;
	    } catch (IllegalMonitorStateException e) {
		System.err.println("I should own the lock but I DON'T");
		Thread.dumpStack();
		throw new NetIbisException("I should own the lock but I DON'T");
	    }
	}
    }


    private void checkUnlocked() throws NetIbisException {
	if (DEBUG_LOCK) {
	    try {
		notify();
		System.err.println("I shouldn't own the lock but I DO");
		Thread.dumpStack();
		throw new NetIbisException("I shouldn't own the lock but I DO");
	    } catch (IllegalMonitorStateException e) {
		return;
	    }
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
    RelInput(NetPortType pt, NetDriver driver, NetIO up, String context) throws NetIbisException {
	super(pt, driver, up, context);

	relDriver = (Driver)driver;

	if (RANDOM_DISCARDS > 0.0) {
	    discardRandom = new java.util.Random();
	}
    }


    /*
     * {@inheritDoc}
     */
    public synchronized void setupConnection(NetConnection cnx)
	    throws NetIbisException {

	spn = cnx.getNum();

	/* Main connection */
	NetInput dataInput = this.dataInput;
	if (dataInput == null) {
	    if (subDriver == null) {
		String subDriverName = getMandatoryProperty("Driver");
		subDriver = driver.getIbis().getDriver(subDriverName);
	    }

	    dataInput = newSubInput(subDriver, "data");
	    this.dataInput = dataInput;
	}

	dataInput.setupConnection(cnx);

	headerStart = dataInput.getHeadersLength();
	ackStart     = headerStart + NetConvert.INT_SIZE;
	dataOffset   = ackStart + RelConstants.headerLength;
	mtu          = dataInput.getMaximumTransfertUnit();

	if (subFactory == null) {
	    subFactory = new NetBufferFactory(mtu, new RelReceiveBufferFactoryImpl());
	    dataInput.setBufferFactory(subFactory);
	    if (true || DEBUG) {
		System.err.println(this + ": ====================== Select BufferFactory " + subFactory + " for my dataInput " +
				   dataInput);
	    }
	} else {
	    subFactory.setMaximumTransferUnit(mtu);
	}

	try {
            NetServiceLink link = cnx.getServiceLink();
            ObjectOutputStream os = new ObjectOutputStream(link.getOutputSubStream(this, "rel"));
            os.writeObject(relDriver.getIbis().identifier());
	    os.flush();

	    ObjectInputStream  is = new ObjectInputStream (link.getInputSubStream (this, "rel"));
	    IbisIdentifier partnerId = (IbisIdentifier)is.readObject();
	    partnerIndex = is.readInt();
	    windowSize = is.readInt();
            is.close();
            
	    relDriver.registerInputConnection(this, partnerId);

	    /* Reverse connection */
	    NetOutput controlOutput = this.controlOutput;

	    if (controlOutput == null) {
		    controlOutput = newSubOutput(subDriver, "control");
		    this.controlOutput = controlOutput;
		    if (DEBUG) {
			System.err.println("My control output is " + controlOutput);
		    }
	    }

	    controlOutput.setupConnection(new NetConnection(cnx, new Integer(-spn.intValue() - 1)));
	    controlHeaderStart = controlOutput.getHeadersLength();

	    if (controlFactory == null) {
		controlFactory = new NetBufferFactory(mtu, new RelSendBufferFactoryImpl());
		controlOutput.setBufferFactory(controlFactory);
	    } else {
		controlFactory.setMaximumTransferUnit(mtu);
	    }

	    os.writeInt(1);
            os.close();
	} catch (java.io.IOException e) {
	    System.err.println("Catch exception " + e);
	    e.printStackTrace();
	    throw new NetIbisException(e);
	} catch (java.lang.ClassNotFoundException e) {
	    throw new NetIbisException(e);
	}
    }


    public int getHeadersLength() {
	return dataOffset;
    }


    // Call this non-synchronized
    private void handlePiggy(byte[] data, int offset)
	    throws NetIbisException {

	checkUnlocked();

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
    private boolean fillAck(byte[] data, int offset, boolean always)
	    throws NetIbisException {

	checkLocked();

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
    private void sendExplicitAck(boolean always) throws NetIbisException {

	checkLocked();

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
	    throws NetIbisException {

	checkLocked();

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
	    throws NetIbisException {

	checkLocked();

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


    /* Call this if you know there is a packet because dataInput.poll() has
     * succeeded. */
    // Call this non-synchronized
    private void receiveDataPacket() throws NetIbisException {

	checkUnlocked();

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


    // No need to call this synchronized or non-synchronized
    private boolean pollDataInput(boolean block) throws NetIbisException {
	boolean dataPending;
	while (true) {
	    dataPending = dataInput.poll() != null;
	    if (dataPending || ! block) {
		return dataPending;
	    }
	    Thread.yield();
	}
    }


    // No need to call this synchronized or non-synchronized
    private void pollQueue() {
	if (front != null && front.fragCount == nextDeliver) {
	    initReceive();
	    activeNum = spn;
	    if (DEBUG) {
		System.err.println("Initialize " + this + " from poll");
		System.err.println("Poll: return my activeNum " + activeNum);
		// Thread.dumpStack();
	    }
	}
    }


    /**
     * {@inheritDoc}
     */
    // Call this non-synchronized
    public Integer poll() throws NetIbisException {

	checkUnlocked();

	if (dataInput == null) {
	    return null;
	}

	activeNum = null;

	pollQueue();
	if (activeNum != null) {
	    /* Seems there were still some queue packets pending */
	    return activeNum;
	}

	if (pollDataInput(false)) {
	    /* There is something to receive from the dataInput. Take a look */
	    receiveDataPacket();
	    pollQueue();
	}

	if (false && DEBUG) {
	    System.err.println(this + ": poll() returns " + activeNum);
	}

	return activeNum;
    }


    /* Block until we have got the buffer we want */
    // Call this synchronized
    private RelReceiveBuffer dequeueReceiveBuffer() throws NetIbisException {

	checkLocked();

	while (front == null || front.fragCount != nextDeliver) {
	    pollDataInput(true);
	    receiveDataPacket();
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
    private void handleReceiveContinuations() throws NetIbisException {

	checkLocked();

	if (waitingForCount == nextDeliver) {
	    notifyAll();
	}
    }


    /**
     * {@inheritDoc}
     */
    synchronized
    public NetReceiveBuffer receiveByteBuffer(int length)
	    throws NetIbisException {
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
    void pushAck(byte[] data, int offset) throws NetIbisException {
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
    public void finish() throws NetIbisException {
	    super.finish();
	    dataInput.finish();
    }


    /**
     * {@inheritDoc}
     */
    public void free() throws NetIbisException {
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
	}
	if (controlOutput != null) {
	    controlOutput.free();
	}

	super.free();
    }


    synchronized public void close(Integer num) throws NetIbisException {
            // to implement
            //
            // - 'num' is the is the Integer identifier of the connection to close
            // - close should not complain is the connection is already closed
            // - close must be synchronized with its counterpart 'setupconnection'
            // - close should not block if possible (except for sync with 
            //   setupconnection)
            // - close should not free the input/output objects which can be used
            //   for subsequent new connections
            // - close is supposed to be called by the NetIbis port layer 
            //   when the peer port is closed
            // - close is also supposed to be called by the user once the IPL
            //   provide the feature, to remove a connection from the connection set
            //   of the port.
    }

}
