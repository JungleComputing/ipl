package ibis.impl.net;

import ibis.ipl.Replacer;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Provide set of attributes describing a NetIbis connection.
 */
public final class NetConnection {

        /**
         * Reference the {@link NetSendPort} or {@link NetReceivePort}
         * which created this connection, and from which the {@link
         * NetPortType} is 'inherited'.
         */
        private NetPort                  port           = null;

        /**
         * Store the connection identifier.
         *
         * This identifier should be unique among the {@link #port}'s connections.
         */
        private Integer                  num            = null;

        /**
         * Store the identifier of the local or peer send port.
         */
        private NetSendPortIdentifier    sendId         = null;

        /**
         * Store the identifier of the local or peer receive port.
         */
        private NetReceivePortIdentifier receiveId      = null;

        /**
         * Reference the service link dedicated to this connection.
         */
        private NetServiceLink           serviceLink    = null;

        /**
         * Replacer. Allows for object replacement before serialization.
         */
        private Replacer                 replacer       = null;

	/**
	 * Manage closing of the streams that belong to this connection.
	 * Ensure that messages are read up to the closeSeqno before the
	 * streams are actually closed.
	 * TODO: check what this does to abnormal closing (link failure etc).
	 */
	public long			msgSeqno;
	public long			closeSeqno;

	public int			regularClosers = 0;

	private DataOutputStream	disconnect_os;

	private DataInputStream		disconnect_is;
	private Thread			disconnectThread;


        /**
         * Construct the set of connection attributes.
         *
         * The actual network connection must have already been established.
         *
         * @param port the {@link NetSendPort} or the {@link
         * NetReceivePort} that owns this connection.
         * @param num the connection identifier.
         * @param sendId the send-side port identifier.
         * @param receiveId the receive-side port identifier.
         * @param serviceLink a reference to the connection's service link.
         * @param replacer allows for object replacement before serialization.
         */
        public NetConnection(NetPort                  port       ,
                             Integer                  num        ,
                             NetSendPortIdentifier    sendId     ,
                             NetReceivePortIdentifier receiveId  ,
                             NetServiceLink           serviceLink,
			     long                     startSeqno,
                             Replacer                 replacer) {
	    this.port        = port       ;
	    this.num         = num        ;
	    this.sendId      = sendId     ;
	    this.receiveId   = receiveId  ;
	    this.serviceLink = serviceLink;
	    this.msgSeqno    = startSeqno;
	    this.closeSeqno  = Long.MAX_VALUE;
	    this.replacer    = replacer;

	    try {
		disconnect_os = new DataOutputStream(serviceLink.getOutputSubStream("disconnect"));

		disconnect_is = new DataInputStream(serviceLink.getInputSubStream("disconnect"));
		disconnectThread = new DisconnectThread();
		disconnectThread.setName(this + " disconnect watcher");
		disconnectThread.setDaemon(true);
		disconnectThread.start();
	    } catch (IOException e) {
		throw new Error("Cannot establish disconnection streams");
	    }
	}

        /**
         * Construct the set of connection attributes.
         *
         * The actual network connection must have already been established.
         *
         * @param port the {@link NetSendPort} or the {@link
         * NetReceivePort} that owns this connection.
         * @param num the connection identifier.
         * @param sendId the send-side port identifier.
         * @param receiveId the receive-side port identifier.
         * @param serviceLink a reference to the connection's service link.
         */
        public NetConnection(NetPort                  port       ,
                             Integer                  num        ,
                             NetSendPortIdentifier    sendId     ,
                             NetReceivePortIdentifier receiveId  ,
                             NetServiceLink           serviceLink,
			     long                     startSeqno) {
	    this(port, num, sendId, receiveId, serviceLink, startSeqno, null);
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
        public NetConnection(NetConnection model ,
                             Integer       newnum) {
                this(model.port, newnum, model.sendId, model.receiveId, model.serviceLink, model.msgSeqno);
        }

        /**
         * Return the owner of this connection.
         *
         * @return the {@link NetSendPort} or the {@link NetReceivePort} that owns this connection.
         */
        public synchronized NetPort getPort() {
                return port;
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
         * Return the {@link NetSendPort} identifier.
         *
         * @return the {@link NetSendPort} identifier.
         */
        public synchronized NetSendPortIdentifier getSendId() {
                return sendId;
        }

        /**
         * Return the {@link NetReceivePort} identifier.
         *
         * @return the {@link NetReceivePort} identifier.
         */
        public synchronized NetReceivePortIdentifier getReceiveId() {
                return receiveId;
        }

        /**
         * Return the reference to the connection's service link.
         *
         * @return the reference to the connection's service link.
         */
        public synchronized NetServiceLink getServiceLink(){
                return serviceLink;
        }

        public synchronized Replacer getReplacer() {
                return replacer;
        }


	private class DisconnectThread extends Thread {

	    public void run() {
		try {
		    NetConnection.this.closeSeqno = disconnect_is.readLong();
// System.err.println(this + ": receive closeSeqno " + closeSeqno);
		    // disconnect_is.close();
		    port.closeFromRemote(NetConnection.this);
		} catch (IOException e) {
		    /* If the service connection breaks, give up all. */
		    if (port instanceof NetReceivePort) {
			NetConnection.this.closeSeqno = 0;
			port.closeFromRemote(NetConnection.this);
		    }
		}
	    }

	}

	public void disconnect(long closeSeqno) throws IOException {
// System.err.println(this + ": send closeSeqno " + closeSeqno);
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

                port        = null;
                num         = null;
                sendId      = null;
                receiveId   = null;
                serviceLink = null;
        }
}
