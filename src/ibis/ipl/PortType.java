package ibis.ipl;

import java.io.IOException;

/**
 * A <code>PortType</code> is used to create send and receive ports.
 * A <code>PortType</code> can be created using the
 * {@link Ibis#createPortType(String, StaticProperties)} method. 
 * Support for connection downcalls can be explicitly turned on and off, because
 * it might incur some overhead. Moreover, if downcalls are used,
 * the amount of administration that must be kept is dependent on the
 * frequency of the user downcalls. If the user does never do a downcall,
 * the administration is kept indefinitely
 */

public abstract class PortType {

    /** Counter for anonymous ports. */
    private int anon_counter;

    /**
     * Returns the name given to this PortType upon creation. 
     *
     * @return the name of this port type.
     */
    public abstract String name();

    /**
     * Returns the properties given to this PortType upon creation. 
     *
     * @return the static properties of this port type.
     */
    public abstract StaticProperties properties();

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
            if (!properties().isProp("communication", "ConnectionUpcalls")) {
                throw new IbisConfigurationException(
                        "no connection upcalls requested for this port type");
            }
        }
        if (connectionDowncalls) {
            if (!properties().isProp("communication", "ConnectionDowncalls")) {
                throw new IbisConfigurationException(
                        "no connection downcalls requested for this port type");
            }
        }
        if (name == null) {
            name = name() + " send port " + anon_counter++;
        }

        return createSendPort(name, cU, connectionDowncalls);
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
     */
    protected abstract SendPort createSendPort(String name,
            SendPortConnectUpcall cU, boolean connectionDowncalls)
            throws IOException;

    /**
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with explicit receipt communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     *
     * @param name the name of this receiveport.
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
     * @param name the name of this receiveport.
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
     * @param name the name of this receiveport.
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
     * @param name the name of this receiveport.
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
     * @param name the name of this receiveport.
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
     * @param name the name of this receiveport.
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
            name = name() + " receive port " + anon_counter++;
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
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    protected abstract ReceivePort createReceivePort(String name, Upcall u,
            ReceivePortConnectUpcall cU, boolean connectionDowncalls)
            throws IOException;

    /**
     * The hashCode method is mentioned here just as a reminder that an
     * implementation must probably redefine it, because two objects
     * representing the same porttype must result in the same hashcode
     * (and compare equal).
     * To explicitly specify it here does not help,
     * because java.lang.Object already implements it,
     * but, anyway, here it is.
     * @return the hashcode.
     */
    public abstract int hashCode();

    /**
     * The equals method is mentioned here just as a reminder that an
     * implementation must probably redefine it, because two objects
     * representing the same porttype must compare equal (and result
     * in the same hashcode).
     * To explicitly specify it here does not help,
     * because java.lang.Object already implements it,
     * but, anyway, here it is.
     * @return the result of the comparison.
     * @param other the object to compare with.
     */
    public abstract boolean equals(Object other);
}