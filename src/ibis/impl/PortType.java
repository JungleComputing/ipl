/* $Id$ */

package ibis.impl;

import ibis.ipl.CapabilitySet;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.Upcall;

import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * Implementation of the {@link ibis.ipl.PortType} interface, to be extended
 * by specific Ibis implementations.
 */
public abstract class PortType implements ibis.ipl.PortType,
        ibis.ipl.PredefinedCapabilities {

    /** Debugging output. */
    private static final Logger logger = Logger.getLogger("ibis.impl.PortType");

    /** Counter for allocating names for anonymous sendports. */
    private static int send_counter = 0;

    /** Counter for allocating names for anonymous receiveports. */
    private static int receive_counter = 0;

    /** The capabilities for this port type. */
    private final CapabilitySet capabilities;

    /** The serialization used for this port type. */
    public final String serialization;

    /** Replacer for object output streams. */
    public final Class replacerClass;

    /** The Ibis instance that created this port type. */
    public Ibis ibis;

    /** Set when messages are numbered. */
    public final boolean numbered;

    /** Set when the port type supports OneToMany communication. */
    public final boolean oneToMany;

    /** Set when the port type supports ManyToOne communication. */
    public final boolean manyToOne;

    /**
     * Constructs a <code>PortType</code> with the specified parameters.
     * @param ibis the ibis instance.
     * @param p the capabilities for the <code>PortType</code>.
     * @exception IbisConfigurationException is thrown when there is some
     * inconsistency in the specified capabilities.
     */
    protected PortType(Ibis ibis, CapabilitySet p) {
        this.ibis = ibis;
    	this.capabilities = p;
   	     
        numbered = p.hasCapability(COMMUNICATION_NUMBERED);
        if (p.hasCapability(SERIALIZATION_DATA)) {
            serialization = "data";
        } else if (p.hasCapability(SERIALIZATION_OBJECT)) {
            serialization = "object";
        } else {
            serialization = "byte";
        }
        if (serialization.equals("byte") && numbered) {
            throw new IbisConfigurationException(
                    "Numbered communication is not supported on byte "
                    + "serialization streams");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Created PortType with capabilities " + p);
        }

        this.oneToMany = capabilities.hasCapability(CONNECTION_ONE_TO_MANY);
        this.manyToOne = capabilities.hasCapability(CONNECTION_MANY_TO_ONE);

        String replacerName = capabilities.getCapability("serialization.replacer");

        if (replacerName != null) {
            try {
                replacerClass = Class.forName(replacerName);
            } catch(Exception e) {
                throw new IbisConfigurationException(
                        "Could not locate replacer class " + replacerName);
            }
            if (replacerName != null
                && ! capabilities.hasCapability(SERIALIZATION_OBJECT)
                && ! capabilities.hasCapability(SERIALIZATION_STRICT_OBJECT)) {
                throw new IbisConfigurationException(
                       "Object replacer specified but no object serialization");
            }
        } else {
            replacerClass = null;
        }
    }

    public CapabilitySet capabilities() {
        return capabilities;
    }

    public ibis.ipl.SendPort createSendPort() throws IOException {
        return createSendPort(null, null, false);
    }

    public ibis.ipl.SendPort createSendPort(String name) throws IOException {
        return createSendPort(name, null, false);
    }

    public ibis.ipl.SendPort createSendPort(boolean connectionDowncalls)
            throws IOException {
        return createSendPort(null, null, connectionDowncalls);
    }

    public ibis.ipl.SendPort createSendPort(String name,
            boolean connectionDowncalls) throws IOException {
        return createSendPort(name, null, connectionDowncalls);
    }

    public ibis.ipl.SendPort createSendPort(String name,
            SendPortDisconnectUpcall cU) throws IOException {
        return createSendPort(name, cU, false);
    }

    /**
     * Creates a {@link ibis.ipl.SendPort} of this <code>PortType</code>.
     *
     * @param name the name of this sendport.
     * @param cU object implementing the
     * {@link SendPortDisconnectUpcall#lostConnection(ibis.ipl.SendPort,
     * ReceivePortIdentifier, Throwable)} method.
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections
     * downcall.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    private ibis.ipl.SendPort createSendPort(String name,
            SendPortDisconnectUpcall cU, boolean connectionDowncalls)
            throws IOException {
        if (cU != null) {
            if (! capabilities.hasCapability(CONNECTION_UPCALLS)) {
                throw new IbisConfigurationException(
                        "no connection upcalls requested for this port type");
            }
        }
        if (connectionDowncalls) {
            if (!capabilities.hasCapability(CONNECTION_DOWNCALLS)) {
                throw new IbisConfigurationException(
                        "no connection downcalls requested for this port type");
            }
        }
        if (name == null) {
            synchronized(this.getClass()) {
                name = "anonymous send port " + send_counter++;
            }
        }

        return doCreateSendPort(name, cU, connectionDowncalls);
    }

    /**
     * Creates a {@link ibis.ipl.SendPort} of this <code>PortType</code>.
     *
     * @param name the name of this sendport.
     * @param cU object implementing the
     * {@link SendPortDisconnectUpcall#lostConnection(ibis.ipl.SendPort,
     * ReceivePortIdentifier, Throwable)} method.
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections
     * downcall.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    protected abstract ibis.ipl.SendPort doCreateSendPort(String name,
            SendPortDisconnectUpcall cU, boolean connectionDowncalls)
            throws IOException;

    public ibis.ipl.ReceivePort createReceivePort(String name)
            throws IOException {
        return createReceivePort(name, null, null, false);
    }

    public ibis.ipl.ReceivePort createReceivePort(String name,
            boolean connectionDowncalls) throws IOException {
        return createReceivePort(name, null, null, connectionDowncalls);
    }

    public ibis.ipl.ReceivePort createReceivePort(String name, Upcall u)
            throws IOException {
        return createReceivePort(name, u, null, false);
    }

    public ibis.ipl.ReceivePort createReceivePort(String name, Upcall u,
            boolean connectionDowncalls) throws IOException {
        return createReceivePort(name, u, null, connectionDowncalls);
    }

    public ibis.ipl.ReceivePort createReceivePort(String name,
            ReceivePortConnectUpcall cU) throws IOException {
        return createReceivePort(name, null, cU, false);
    }

    public ibis.ipl.ReceivePort createReceivePort(String name, Upcall u,
            ReceivePortConnectUpcall cU) throws IOException {
        return createReceivePort(name, u, cU, false);
    }

    /** 
     * Creates a named {@link ibis.ipl.ReceivePort} of this
     * <code>PortType</code>, with upcall based communication.
     * New connections will not be accepted until
     * {@link ibis.ipl.ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     * When a new connection request arrives, or when a connection is lost,
     * a ConnectUpcall is performed.
     *
     * @param name the unique name of this receiveport (or <code>null</code>,
     *    in which case the port is created anonymously and is not bound
     *    in the registry).
     * @param u the upcall handler.
     * @param cU object implementing <code>gotConnection</code>() and
     * <code>lostConnection</code>() upcalls.
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections and
     * newConnections downcalls.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    private ibis.ipl.ReceivePort createReceivePort(String name, Upcall u,
            ReceivePortConnectUpcall cU, boolean connectionDowncalls)
            throws IOException {
        CapabilitySet p = capabilities;
        if (cU != null) {
            if (!p.hasCapability(CONNECTION_UPCALLS)) {
                throw new IbisConfigurationException(
                        "no connection upcalls requested for this port type");
            }
        }
        if (connectionDowncalls) {
            if (!p.hasCapability(CONNECTION_DOWNCALLS)) {
                throw new IbisConfigurationException(
                        "no connection downcalls requested for this port type");
            }
        }
        if (u != null) {
            if (!p.hasCapability(RECEIVE_AUTO_UPCALLS)
                    && !p.hasCapability(RECEIVE_POLL_UPCALLS)) {
                throw new IbisConfigurationException(
                        "no message upcalls requested for this port type");
            }
        } else {
            if (!p.hasCapability(RECEIVE_EXPLICIT)) {
                throw new IbisConfigurationException(
                        "no explicit receive requested for this port type");
            }
        }
        if (name == null) {
            synchronized(this.getClass()) {
                name = "anonymous receive port " + receive_counter++;
            }
        }

        return doCreateReceivePort(name, u, cU, connectionDowncalls);
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof PortType)) {
            return false;
        }
            return capabilities.equals(((PortType) other).capabilities);
        }

    public int hashCode() {
        return capabilities.hashCode();
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Protected methods, to be implemented by Ibis implementations.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /** 
     * Creates a named {@link ibis.ipl.ReceivePort} of this
     * <code>PortType</code>, with upcall based communication.
     * New connections will not be accepted until
     * {@link ibis.ipl.ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     * When a new connection request arrives, or when a connection is lost,
     * a ConnectUpcall is performed.
     *
     * @param name the name of this receiveport.
     * @param u the upcall handler.
     * @param cU object implementing <code>gotConnection</code>() and
     * <code>lostConnection</code>() upcalls.
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections and
     * newConnections downcalls.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    protected abstract ibis.ipl.ReceivePort doCreateReceivePort(String name,
            Upcall u, ReceivePortConnectUpcall cU, boolean connectionDowncalls)
            throws IOException;
}
