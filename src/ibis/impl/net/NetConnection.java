/* $Id$ */

package ibis.impl.net;

import ibis.ipl.Replacer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.Map;

/**
 * Provide set of attributes describing a NetIbis connection.
 */
public final class NetConnection {

    private final static boolean DEBUG = false;

    /**
     * Reference the {@link ibis.impl.net.NetSendPort}
     * which created this connection, and from
     * which the {@link NetPortType} is 'inherited', or <code>null</code>.
     */
    private NetSendPort sendPort = null;

    /**
     * Reference the or {@link ibis.impl.net.NetReceivePort} which created
     * this connection, and from which the {@link NetPortType} is 'inherited',
     * or <code>null</code>.
     */
    private NetReceivePort receivePort = null;

    /**
     * Store the connection identifier.
     *
     * This identifier should be unique among the port's connections.
     */
    private Integer num = null;

    /**
     * Store the identifier of the local or peer send port.
     */
    private NetSendPortIdentifier sendId = null;

    /**
     * Store the identifier of the local or peer receive port.
     */
    private NetReceivePortIdentifier receiveId = null;

    /**
     * Reference the service link dedicated to this connection.
     */
    private NetServiceLink serviceLink = null;

    /**
     * Replacer. Allows for object replacement before serialization.
     */
    private Replacer replacer = null;

    /**
     * Manage closing of the streams that belong to this connection.
     * Ensure that messages are read up to the closeSeqno before the
     * streams are actually closed.
     * TODO: check what this does to abnormal closing (link failure etc).
     */
    public long closeSeqno;

    /**
     * Messages over this connection are counted so we can close gracefully.
     * Close is only allowed if {link #msgSeqno} equals {link #closeSeqno}.
     */
    public long msgSeqno;

    int regularClosers = 0;

    private DataOutputStream disconnect_os;

    private DataInputStream disconnect_is;

    private DisconnectThread disconnectThread;

    /**
     * Construct the set of connection attributes.
     *
     * The actual network connection must have already been established.
     *
     * @param port the {@link ibis.impl.net.NetSendPort} that owns this
     * connection.
     * @param num the connection identifier.
     * @param sendId the send-side port identifier.
     * @param receiveId the receive-side port identifier.
     * @param serviceLink a reference to the connection's service link.
     * @param replacer allows for object replacement before serialization.
     */
    public NetConnection(NetSendPort port, Integer num,
            NetSendPortIdentifier sendId, NetReceivePortIdentifier receiveId,
            NetServiceLink serviceLink, long startSeqno, Replacer replacer) {
        this(port, null, num, sendId, receiveId, serviceLink, startSeqno,
                replacer);
    }

    /**
     * Construct the set of connection attributes.
     *
     * The actual network connection must have already been established.
     *
     * @param port the {@link ibis.impl.net.NetReceivePort} that owns this
     * connection.
     * @param num the connection identifier.
     * @param sendId the send-side port identifier.
     * @param receiveId the receive-side port identifier.
     * @param serviceLink a reference to the connection's service link.
     * @param replacer allows for object replacement before serialization.
     */
    public NetConnection(NetReceivePort port, Integer num,
            NetSendPortIdentifier sendId, NetReceivePortIdentifier receiveId,
            NetServiceLink serviceLink, long startSeqno, Replacer replacer) {
        this(null, port, num, sendId, receiveId, serviceLink, startSeqno,
                replacer);
    }

    /**
     * Construct the set of connection attributes.
     *
     * The actual network connection must have already been established.
     *
     * @param sport the {@link ibis.impl.net.NetSendPort} that owns this
     * connection, or <code>null</code>.
     * @param rport the {@link ibis.impl.net.NetReceivePort} that owns this
     * connection, or <code>null</code>.
     * @param num the connection identifier.
     * @param sendId the send-side port identifier.
     * @param receiveId the receive-side port identifier.
     * @param serviceLink a reference to the connection's service link.
     * @param replacer allows for object replacement before serialization.
     */
    private NetConnection(NetSendPort sport, NetReceivePort rport, Integer num,
            NetSendPortIdentifier sendId, NetReceivePortIdentifier receiveId,
            NetServiceLink serviceLink, long startSeqno, Replacer replacer) {
        this.sendPort = sport;
        this.receivePort = rport;
        this.num = num;
        this.sendId = sendId;
        this.receiveId = receiveId;
        this.serviceLink = serviceLink;
        this.msgSeqno = startSeqno;
        this.closeSeqno = Long.MAX_VALUE;
        this.replacer = replacer;

        try {
            disconnect_os = new DataOutputStream(
                    serviceLink.getOutputSubStream("disconnect"));
            NetServiceInputStream sis = serviceLink.getInputSubStream(
                    "disconnect");
            disconnect_is = new DataInputStream(sis);
            disconnectThread = new DisconnectThread();
            sis.registerPopup(disconnectThread);
        } catch (IOException e) {
            throw new Error("Cannot establish disconnection streams");
        }
    }

    /**
     * Construct a dummy connection object.
     *
     * Useful for drivers that manage their own set of internal
     * network connections (e.g. forward 'data' and reversed 'ack'
     * connection in the reliability Driver) or drivers that
     * perform some kind of multiplexing.
     *
     * Both the model connection and the new connection will share attributes
     * with the exception of the connection identifier. In particular,
     * the service link <B>will</B> be shared.
     *
     * @param model the model connection that will share its
     * attributes with the new cloned dummy connection. This model
     * connection may already be a clone of another connection.
     *
     * @param newnum the identifier of the new dummy connection.
     * The namespace of this identifier is not shared with the
     * identifier's namespace of the <I>master copy</I> of this
     * connection.
     */
    public NetConnection(NetConnection model, Integer newnum) {
        this(model.sendPort, model.receivePort, newnum, model.sendId,
                model.receiveId, model.serviceLink, model.msgSeqno, null);
    }

    /**
     * Return the connection identifier.
     *
     * @return the connection identifier
     */
    public synchronized Integer getNum() {
        return num;
    }

    /**
     * Return the properties of the creating port.
     *
     * @return the properties.
     */
    public Map properties() {
        if (sendPort != null) {
            return sendPort.properties();
        }
        if (receivePort != null) {
            return receivePort.properties();
        }
        return null;
    }

    /**
     * Return the {@link ibis.impl.net.NetSendPort} identifier.
     *
     * @return the {@link ibis.impl.net.NetSendPort} identifier.
     */
    public synchronized NetSendPortIdentifier getSendId() {
        return sendId;
    }

    /**
     * Return the {@link ibis.impl.net.NetReceivePort} identifier.
     *
     * @return the {@link ibis.impl.net.NetReceivePort} identifier.
     */
    public synchronized NetReceivePortIdentifier getReceiveId() {
        return receiveId;
    }

    /**
     * Return the reference to the connection's service link.
     *
     * @return the reference to the connection's service link.
     */
    public synchronized NetServiceLink getServiceLink() {
        return serviceLink;
    }

    /**
     * Return the {@link Replacer} object.
     *
     * @return the {@link Replacer} object
     */
    public synchronized Replacer getReplacer() {
        return replacer;
    }

    private class DisconnectThread implements NetServicePopupThread {

        private String name = this + "-disconnect thread";

        public void callBack() throws IOException {
            NetConnection.this.closeSeqno = disconnect_is.readLong();
            if (DEBUG) {
                System.err.println(this + ": receive closeSeqno " + closeSeqno);
            }
            // disconnect_is.close();
            if (sendPort != null) {
                sendPort.closeFromRemote(NetConnection.this);
            }
            if (receivePort != null) {
                receivePort.closeFromRemote(NetConnection.this);
            }
        }

        public String getName() {
            return name;
        }

    }

    /**
     * Output sends a disconnect request to the matching Input. Disconnect
     * is graceful since the input closes only after it has processed sent
     * messages up to sequence number {@link #closeSeqno}.
     *
     * @param closeSeqno the matching input will close only after it has
     * 		processed messages up to this sequence number.
     */
    public void disconnect(long closeSeqno) throws IOException {
        if (DEBUG) {
            System.err.println(this + ": send closeSeqno " + closeSeqno);
        }
        disconnect_os.writeLong(closeSeqno);
        disconnect_os.flush();
        // disconnect_os.close();
    }

    /**
     * Closes the connection.
     *
     * This method also closes the connection's service link.
     *
     * @exception IOException if the operation fails.
     */
    public synchronized void close() throws IOException {
        if (serviceLink != null) {
            serviceLink.close();
        }

        sendPort = null;
        receivePort = null;
        num = null;
        sendId = null;
        receiveId = null;
        serviceLink = null;
    }
}
