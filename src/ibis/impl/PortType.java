/* $Id: PortType.java 4871 2006-12-06 16:54:07Z ceriel $ */

package ibis.impl;

import ibis.io.Replacer;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.StaticProperties;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.Upcall;
import ibis.util.GetLogger;

import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * An Ibis implementation can choose to extend this class to implement
 * the PortType interface.
 */
public abstract class PortType implements ibis.ipl.PortType {

    private static final Logger logger
            = GetLogger.getLogger("ibis.impl.PortType");

    /** Counter for allocating names for anonymous ports. */
    private static int anon_counter = 0;

    /** The properties for this port type. */
    protected StaticProperties props;

    /** The serialization used for this port type. */
    protected String serialization;

    /** Replacer for object output streams. */
    protected Class replacerClass = null;

    /** The Ibis instance that created this port type. */
    protected Ibis ibis;

    public PortType(Ibis ibis, StaticProperties p)
            throws PortMismatchException {
        this.ibis = ibis;
    	this.props = p;
        boolean numbered = p.isProp("communication", "Numbered");

        serialization = p.find("Serialization");
        if (serialization == null) {
            serialization = "sun";
        }
        if (serialization.equals("byte") && numbered) {
            throw new PortMismatchException(
                    "Numbered communication is not supported on byte "
                    + "serialization streams");
        }

        logger.debug("Created PortType with properties " + p);

        String replacerName = props.find("serialization.replacer");

        if (replacerName != null) {
            if (! props.isProp("serialization", "sun") &&
                ! props.isProp("serialization", "object") &&
                ! props.isProp("serialization", "ibis")) {
                throw new IbisConfigurationException(
                       "Object replacer specified but no object serialization");
            }
            try {
                replacerClass = Class.forName(replacerName);
            } catch(Exception e) {
                throw new IbisConfigurationException(
                        "Could not locate replacer class " + replacerName);
            }
        }
    }

    public StaticProperties properties() {
        // TODO: return a copy?
        return props;
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
            SendPortConnectUpcall cU) throws IOException {
        return createSendPort(name, cU, false);
    }

    /**
     * Creates a {@link ibis.ipl.SendPort} of this <code>PortType</code>.
     *
     * @param name the name of this sendport.
     * @param cU object implementing the
     * {@link SendPortConnectUpcall#lostConnection(ibis.ipl.SendPort,
     * ReceivePortIdentifier, Exception)} method.
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
            SendPortConnectUpcall cU, boolean connectionDowncalls)
            throws IOException {
        if (cU != null) {
            if (! props.isProp("communication", "ConnectionUpcalls")) {
                throw new IbisConfigurationException(
                        "no connection upcalls requested for this port type");
            }
        }
        if (connectionDowncalls) {
            if (!props.isProp("communication", "ConnectionDowncalls")) {
                throw new IbisConfigurationException(
                        "no connection downcalls requested for this port type");
            }
        }
        if (name == null) {
            synchronized(this.getClass()) {
                name = "anonymous send port " + anon_counter++;
            }
        }

        return doCreateSendPort(name, cU, connectionDowncalls);
    }

    /**
     * Creates a {@link ibis.ipl.SendPort} of this <code>PortType</code>.
     *
     * @param name the name of this sendport.
     * @param cU object implementing the
     * {@link SendPortConnectUpcall#lostConnection(ibis.ipl.SendPort,
     * ReceivePortIdentifier, Exception)} method.
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections
     * downcall.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    protected abstract ibis.ipl.SendPort doCreateSendPort(String name,
            SendPortConnectUpcall cU, boolean connectionDowncalls)
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
        StaticProperties p = properties();
        if (cU != null) {
            if (!p.isProp("communication", "ConnectionUpcalls")) {
                throw new IbisConfigurationException(
                        "no connection upcalls requested for this port type");
            }
        }
        if (connectionDowncalls) {
            if (!p.isProp("communication", "ConnectionDowncalls")) {
                throw new IbisConfigurationException(
                        "no connection downcalls requested for this port type");
            }
        }
        if (u != null) {
            if (!p.isProp("communication", "AutoUpcalls")
                    && !p.isProp("communication", "PollUpcalls")) {
                throw new IbisConfigurationException(
                        "no message upcalls requested for this port type");
            }
        } else {
            if (!p.isProp("communication", "ExplicitReceipt")) {
                throw new IbisConfigurationException(
                        "no explicit receipt requested for this port type");
            }
        }
        if (name == null) {
            name = ReceivePort.ANONYMOUS;
        }

        return doCreateReceivePort(name, u, cU, connectionDowncalls);
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
     * @param name the name of this receiveport.
     * @param u the upcall handler.
     * @param cU object implementing <code>gotConnection</code>() and
     * <code>lostConnection</code>() upcalls.
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections and
     * newConnections downcalls.
     * @param global set if the port must be registered in the Ibis registry.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    protected abstract ibis.ipl.ReceivePort doCreateReceivePort(String name,
            Upcall u, ReceivePortConnectUpcall cU, boolean connectionDowncalls)
            throws IOException;

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof PortType)) {
            return false;
        }
            return props.equals(((PortType) other).props);
        }

    public int hashCode() {
        return props.hashCode();
    }
}
