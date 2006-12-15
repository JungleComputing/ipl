/* $Id: PortType.java 4871 2006-12-06 16:54:07Z ceriel $ */

package ibis.impl;

import ibis.io.Replacer;
import ibis.ipl.Ibis;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.StaticProperties;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.Upcall;

import java.io.IOException;

/**
 * A <code>PortType</code> represents a class of send and receive
 * ports with specific properties and with a specific role in the program.
 * Each class also
 * serves as the factory to create instances of these ports.
 * Ports can only connect to other ports of the same type.
 * A <code>PortType</code> is created using the
 * {@link Ibis#createPortType(StaticProperties)} method. 
 * <p>
 * Support for connection downcalls can be explicitly turned on and off, because
 * it might incur some overhead. Moreover, if downcalls are used,
 * the amount of administration that must be kept is dependent on the
 * frequency of the user downcalls. If the user never does a downcall,
 * the administration is kept indefinitely.
 * </p>
 */

public abstract class PortType implements ibis.ipl.PortType {

    private static int anon_counter = 0;

    public StaticProperties props;

    public boolean numbered;

    public String serialization;

    public PortType(StaticProperties p)
            throws PortMismatchException {

    	this.props = p;

        numbered = p.isProp("communication", "Numbered");

        serialization = p.find("Serialization");
        if (serialization == null) {
            serialization = "sun";
        }
        if (serialization.equals("byte") && numbered) {
            throw new PortMismatchException(
                    "Numbered communication is not supported on byte "
                    + "serialization streams");
        }
    }

    /**
     * Returns the properties given to this PortType upon creation. 
     *
     * @return the static properties of this port type.
     */
    public StaticProperties properties() {
        // TODO: return a copy?
        return props;
    }

    /**
     * Creates a anonymous {@link SendPort} of this <code>PortType</code>.
     * 
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public SendPort createSendPort() throws IOException {
        return doCreateSendPort(null, null, false);
    }

    /**
     * Creates a named {@link SendPort} of this <code>PortType</code>.
     * The name does not have to be unique.
     *
     * @param name the name of this sendport.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public SendPort createSendPort(String name) throws IOException {
        return doCreateSendPort(name, null, false);
    }

    /**
     * Creates a anonymous {@link SendPort} of this <code>PortType</code>.
     * 
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections
     * downcall.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public SendPort createSendPort(boolean connectionDowncalls)
            throws IOException {
        return doCreateSendPort(null, null, connectionDowncalls);
    }

    /**
     * Creates a named {@link SendPort} of this <code>PortType</code>.
     * The name does not have to be unique.
     *
     * @param name the name of this sendport.
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections
     * downcall.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public SendPort createSendPort(String name, boolean connectionDowncalls)
            throws IOException {
        return doCreateSendPort(name, null, connectionDowncalls);
    }

    /** 
     * Creates a named {@link SendPort} of this <code>PortType</code>.
     * The name does not have to be unique.
     * When a connection is lost, a ConnectUpcall is performed.
     *
     * @param name the name of this sendport.
     * @param cU object implementing the
     * {@link SendPortConnectUpcall#lostConnection(SendPort,
     * ReceivePortIdentifier, Exception)} method.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public SendPort createSendPort(String name, SendPortConnectUpcall cU)
            throws IOException {
        return doCreateSendPort(name, cU, false);
    }

    /**
     * Creates a {@link SendPort} of this <code>PortType</code>.
     *
     * @param name the name of this sendport.
     * @param cU object implementing the
     * {@link SendPortConnectUpcall#lostConnection(SendPort,
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
    private SendPort doCreateSendPort(String name, SendPortConnectUpcall cU,
            boolean connectionDowncalls) throws IOException {
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

        String replacerName = props.find("serialization.replacer");
        Replacer r = null;
        if (replacerName != null) {
            if (! props.isProp("serialization", "sun") &&
                ! props.isProp("serialization", "object") &&
                ! props.isProp("serialization", "ibis")) {
                throw new IbisConfigurationException(
                        "Object replacer specified but no object serialization");
            }
            try {
                Class cl = Class.forName(replacerName);
                r = (Replacer) cl.newInstance();
            } catch(Exception e) {
                throw new IOException("Could not locate or instantiate replacer class "
                        + replacerName);
            }
        }

        return createSendPort(name, cU, connectionDowncalls, r);
    }

    /**
     * Creates a {@link SendPort} of this <code>PortType</code>.
     *
     * @param name the name of this sendport.
     * @param cU object implementing the
     * {@link SendPortConnectUpcall#lostConnection(SendPort,
     * ReceivePortIdentifier, Exception)} method.
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections
     * downcall.
     * @param replacer an object replacer, or <code>null</code>.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    protected abstract SendPort createSendPort(String name, SendPortConnectUpcall cU,
            boolean connectionDowncalls, Replacer replacer)
            throws IOException;

    /**
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with explicit receipt communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     *
     * @param name the unique name of this receiveport (or <code>null</code>,
     *    in which case the port is created anonymously and is not bound
     *    in the registry).
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public ReceivePort createReceivePort(String name) throws IOException {
        return doCreateReceivePort(name, null, null, false);
    }

    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with explicit receipt communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     *
     * @param name the unique name of this receiveport (or <code>null</code>,
     *    in which case the port is created anonymously and is not bound
     *    in the registry).
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections and
     * newConnections downcalls.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public ReceivePort createReceivePort(String name,
            boolean connectionDowncalls) throws IOException {
        return doCreateReceivePort(name, null, null, connectionDowncalls);
    }

    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with upcall based communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     *
     * @param name the unique name of this receiveport (or <code>null</code>,
     *    in which case the port is created anonymously and is not bound
     *    in the registry).
     * @param u the upcall handler.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public ReceivePort createReceivePort(String name, Upcall u)
            throws IOException {
        return doCreateReceivePort(name, u, null, false);
    }

    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with upcall based communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     *
     * @param name the unique name of this receiveport (or <code>null</code>,
     *    in which case the port is created anonymously and is not bound
     *    in the registry).
     * @param u the upcall handler.
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections and
     * newConnections downcalls.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public ReceivePort createReceivePort(String name, Upcall u,
            boolean connectionDowncalls) throws IOException {
        return doCreateReceivePort(name, u, null, connectionDowncalls);
    }

    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with explicit receipt communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     * When a new connection request arrives, or when a connection is lost,
     * a ConnectUpcall is performed.
     *
     * @param name the unique name of this receiveport (or <code>null</code>,
     *    in which case the port is created anonymously and is not bound
     *    in the registry).
     * @param cU object implementing <code>gotConnection</code>() and
     * <code>lostConnection</code>() upcalls.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public ReceivePort createReceivePort(String name,
            ReceivePortConnectUpcall cU) throws IOException {
        return doCreateReceivePort(name, null, cU, false);
    }

    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with upcall based communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
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
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public ReceivePort createReceivePort(String name, Upcall u,
            ReceivePortConnectUpcall cU) throws IOException {
        return doCreateReceivePort(name, u, cU, false);
    }

    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with upcall based communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
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
    private ReceivePort doCreateReceivePort(String name, Upcall u,
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

        return createReceivePort(name, u, cU, connectionDowncalls);
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
    protected abstract ReceivePort createReceivePort(String name, Upcall u,
            ReceivePortConnectUpcall cU, boolean connectionDowncalls)
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
