package ibis.ipl.impl.net;

import ibis.io.Replacer;

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
         * Replacer ?.
         */
        private Replacer                 replacer       = null;

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
         * @param replacer ???
         */
        public NetConnection(NetPort                  port       ,
                             Integer                  num        ,
                             NetSendPortIdentifier    sendId     ,
                             NetReceivePortIdentifier receiveId  ,
                             NetServiceLink           serviceLink,
                             Replacer                 replacer) {
                this.port        = port       ;
                this.num         = num        ;
                this.sendId      = sendId     ;
                this.receiveId   = receiveId  ;
                this.serviceLink = serviceLink;
                this.replacer    = replacer;
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
                             NetServiceLink           serviceLink) {
                this.port        = port       ;
                this.num         = num        ;
                this.sendId      = sendId     ;
                this.receiveId   = receiveId  ;
                this.serviceLink = serviceLink;
                this.replacer    = null;
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
         * with the exception of the {@linkplain #num connection identifier}. In particular,
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
                this(model.port, newnum, model.sendId, model.receiveId, model.serviceLink);
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

        /**
         * Closes the connection.
         *
         * This method also closes the connection's {@linkplain
         * #serviceLink service link}.
         *
         * @exception NetIbisException if the operation fails.
         */
        public synchronized void close() throws NetIbisException {
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
