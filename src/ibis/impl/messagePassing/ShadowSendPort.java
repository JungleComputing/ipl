package ibis.impl.messagePassing;

import ibis.io.Conversion;
import ibis.ipl.PortMismatchException;

import java.io.IOException;
import java.io.StreamCorruptedException;

/**
 * Receiver-side SendPort stub that performs Byte serialization
 */
class ShadowSendPort extends SendPort {

    ReadMessage cachedMessage = null;

    private boolean connect_allowed;

    private ReadFragment cachedFragment = null;

    ByteInputStream in;

    protected int msgcount = Integer.MAX_VALUE;

    private int quitCount;

    private int quitSyncer;

    int msgSeqno = -1; /* Count messages to do fragmentation */

    protected int groupStartSeqno = Integer.MAX_VALUE;

    protected ReceivePort receivePort;

    protected static native void sendConnectAck(int dest, int syncer,
            boolean accept);

    protected static native void sendDisconnectAck(int dest, int syncer,
            boolean accept);

    /* Called from native code */
    static ShadowSendPort createShadowSendPort(byte[] rcvePortBuf,
            byte[] sendPortBuf, int startSeqno, int group, int groupStartSeqno,
            int syncer) throws IOException {

        ReceivePortIdentifier rId;
        SendPortIdentifier sId;

        try {
            rId = (ReceivePortIdentifier) Conversion.byte2object(rcvePortBuf);
            sId = (SendPortIdentifier) Conversion.byte2object(sendPortBuf);
            if (Ibis.DEBUG_RUTGER) {
                System.err.println("Create shadow SendPort (group " + group
                        + ") for sendPort that belongs to Ibis " + sId);
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Cannot read Ids from stream " + e);
        }

        PortType portType = Ibis.myIbis.getPortTypeLocked(sId.type());
        int serializationType = portType.serializationType;
        if (DEBUG) {
            System.err.println("********** Create a ShadowSendPort, my type is "
                            + serializationType);
        }
        switch (serializationType) {
        case PortType.SERIALIZATION_NONE:
            return new ShadowSendPort(rId, sId, startSeqno, group,
                    groupStartSeqno);

        case PortType.SERIALIZATION_SUN:
            return new SerializeShadowSendPort(rId, sId, startSeqno, group,
                    groupStartSeqno, syncer);

        case PortType.SERIALIZATION_IBIS:
        case PortType.SERIALIZATION_DATA:
            return new IbisShadowSendPort(rId, sId, startSeqno, group,
                    groupStartSeqno);

        default:
            throw new Error("No such serialization type " + serializationType);
        }
    }

    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    ShadowSendPort(ReceivePortIdentifier rId, SendPortIdentifier sId,
            int startSeqno, int group, int groupStartSeqno) throws IOException {

        if (DEBUG) {
            System.err.println("In ShadowSendPort.<init>");
        }

        ident = sId;
        Ibis.myIbis.bindSendPort(this, ident.cpu, ident.port);

        receivePort = Ibis.myIbis.lookupReceivePort(rId.port);
        if (!rId.type().equals(ident.type())) {
            System.err.println("********************** "
                    + " ShadowSendPort type does not equal connected "
                    + " ReceivePort type");
            throw new PortMismatchException("Cannot connect send port and "
                    + " receive port of different types: " + type + " <-> "
                    + receivePort.identifier().type());
        }
        this.type = receivePort.type();
        this.msgcount = startSeqno;
        this.groupStartSeqno = groupStartSeqno;

        connect_allowed = receivePort.connect(this);
        if (!connect_allowed) {
            Ibis.myIbis.unbindSendPort(ident.cpu, ident.port);
            System.err.println(this + ": cannot connect to local ReceivePort "
                    + receivePort);
        } else if (group != NO_BCAST_GROUP) {
            if (DEBUG) {
                System.err.println("Bind group " + group + " to port "
                        + rId.port + "; sender " + sId.cpu + " port "
                        + sId.port + " startSeqno " + startSeqno);
            }
            Ibis.myIbis.bindGroup(group, receivePort, this);
            this.group = group;
        }

        in = new ByteInputStream();
        if (DEBUG) {
            System.err.println(Thread.currentThread()
                    + "Created shadow send port " + this + " (" + sId.cpu + ","
                    + sId.port + "), connect to local port " + rId.port);
        }
    }

    /* Called from native */
    static void bindGroup(int src_cpu, byte[] sendPortBuf, int group)
            throws IOException {

        SendPortIdentifier sId;

        try {
            sId = (SendPortIdentifier) Conversion.byte2object(sendPortBuf);
        } catch (ClassNotFoundException e) {
            throw new IOException("Cannot read Ids from stream " + e);
        }
        ShadowSendPort ssp = Ibis.myIbis.lookupSendPort(sId.cpu, sId.port);
        if (ssp == null) {
            throw new IOException("Cannot locate ShadowSendPort " + sId);
        }
        if (DEBUG) {
            ibis.ipl.ReceivePortIdentifier id = ssp.receivePort.identifier();
            ReceivePortIdentifier rid = (ReceivePortIdentifier)id;
            System.err.println("Bind/later group " + group + " to port "
                    + rid + "; sender " + sId.cpu + " port " + sId.port);
        }
        Ibis.myIbis.bindGroup(group, ssp.receivePort, ssp);
    }

    boolean acceptableSeqno(int seqno) {
        return (seqno & ~ByteOutputStream.SEQNO_FRAG_BITS) >= groupStartSeqno;
    }

    ReadMessage getMessage(int seqno) throws IOException {
        ReadMessage msg = cachedMessage;

        if (DEBUG) {
            System.err.println("Get a NONE ReadMessage ");
        }

        if (msg != null) {
            cachedMessage = null;

        } else {
            msg = new ReadMessage(this, receivePort);
            if (DEBUG) {
                System.err.println("Create a -none- ReadMessage " + msg);
            }
        }

        msg.msgSeqno = seqno;
        return msg;
    }

    /* Sun-Serialize streams need a complicated x-phase startup because they
     * start reading in the constructor. Provide a handle to create the
     * Sun-Serialize streams after we have deposited the first msg/fragment
     * in the queue. */
    boolean checkStarted(ReadMessage msg) throws IOException {
        return true;
    }

    void setMsgHandle(ReadMessage msg) {
        in.setMsgHandle(msg);
    }

    ReadFragment getFragment() {
        ReadFragment f = cachedFragment;
        if (f == null) {
            return new ReadFragment();
        }

        cachedFragment = f.next;
        f.next = null;

        return f;
    }

    void putFragment(ReadFragment f) {
        f.next = cachedFragment;
        cachedFragment = f;
    }

    void disconnect() throws IOException {
        // No-op here, provide for subclasses
    }

    private void doDisconnect(int sncer) throws IOException {
        disconnect();
        Ibis.myIbis.unbindSendPort(ident.cpu, ident.port);
        if (group != NO_BCAST_GROUP) {
            Ibis.myIbis.unbindGroup(group, receivePort, this);
            group = NO_BCAST_GROUP;
        }
        groupStartSeqno = Integer.MAX_VALUE;
        receivePort.disconnect(this);
        msgcount = Integer.MAX_VALUE;

        sendDisconnectAck(ident.cpu, sncer, true);
    }

    void tickReceive() throws IOException {
        msgcount++;
        if (msgcount == quitCount) {
            doDisconnect(quitSyncer);
        }
    }

    static void disconnect(byte[] rcvePortId, byte[] sendPortId, int syncer,
            int count) throws IOException {
        ReceivePortIdentifier rId = null;
        SendPortIdentifier sId = null;
        try {
            rId = (ReceivePortIdentifier) Conversion.byte2object(rcvePortId);
            sId = (SendPortIdentifier) Conversion.byte2object(sendPortId);
        } catch (ClassNotFoundException e) {
            throw new IOException("Cannot read Ids from stream " + e);
        }

        ShadowSendPort sp = Ibis.myIbis.lookupSendPort(sId.cpu, sId.port);
        ReceivePort rp = Ibis.myIbis.lookupReceivePort(rId.port);

        if (DEBUG) {
            System.err.println(Thread.currentThread()
                    + "Receive a disconnect call from SendPort " + sp
                    + ", disconnect from RcvePort " + rp + " count " + count
                    + " sp.messageCount " + sp.msgcount);
        }
        if (rp != sp.receivePort) {
            System.err.println("Try to disconnect from a receive port "
                    + " we're not connected to...");
            Thread.dumpStack();
        }
        if (sp.msgcount == count) {
            sp.doDisconnect(syncer);
        } else if (sp.msgcount > count) {
            throw new StreamCorruptedException("This cannot happen...");
        } else {
            sp.quitCount = count;
            sp.quitSyncer = syncer;
        }
    }

}
