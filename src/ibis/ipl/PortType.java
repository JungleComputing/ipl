package ibis.ipl;

import java.io.IOException;

/**
 * A <code>PortType</code> is used to create send and receive ports.
 * A <code>PortType</code> can be created using the
 * {@link Ibis#createPortType(String, StaticProperties)} method. 
 * connectionAdministration can be explicitly turned on and off, because
 * it might incur some overhead. Moreover, if downcalls are used,
 * the amount of administration that must be kept is dependent on the
 * frequency of the user downcalls. If the user does never do a downcall,
 * the administration is kept indefinitely
 */

public interface PortType {

    /**
     * Returns the name given to this PortType upon creation. 
     *
     * @return the name of this port type.
     */
    public String name();

    /**
     * Returns the properties given to this PortType upon creation. 
     *
     * @return the static properties of this port type.
     */
    public StaticProperties properties();

    /**
     * Creates a named {@link SendPort} of this <code>PortType</code>.
     * The name does not have to be unique.
     *
     * @param name the name of this sendport.
     * @param connectionAdministration set when this port must keep
     * connection administration to support the lostConnections and
     * newConnections downcalls.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public SendPort createSendPort(String name,
				   boolean connectionAdministration)
	throws IOException;

    /**
     * Creates a named {@link SendPort} of this <code>PortType</code>.
     * The name does not have to be unique.
     * ConnectionAdministration is disabled.
     *
     * @param name the name of this sendport.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public SendPort createSendPort(String name) throws IOException;

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
     */
    public SendPort createSendPort(String name, SendPortConnectUpcall cU)
	throws IOException;

    /**
     * Creates a anonymous {@link SendPort} of this <code>PortType</code>.
     * ConnectionAdministration is disabled.
     * 
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public SendPort createSendPort() throws IOException;

    /**
     * Creates a anonymous {@link SendPort} of this <code>PortType</code>.
     * 
     * @param connectionAdministration set when this port must keep
     * connection administration to support the lostConnections and
     * newConnections downcalls.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public SendPort createSendPort(boolean connectionAdministration)
	throws IOException;

    /**
     * Creates a anonymous {@link SendPort} of this <code>PortType</code>,
     * with a {@link Replacer}. 
     *
     * @param r an object replacer, used in object serialization.
     * @param connectionAdministration set when this port must keep
     * connection administration to support the lostConnections and
     * newConnections downcalls.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public SendPort createSendPort(Replacer r, boolean connectionAdministration)
	throws IOException;

    /**
     * Creates a anonymous {@link SendPort} of this <code>PortType</code>,
     * with a {@link Replacer}. 
     * ConnectionAdministration is disabled.
     *
     * @param r an object replacer, used in object serialization.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public SendPort createSendPort(Replacer r) throws IOException;

    /**
     * Creates a named {@link SendPort} of this <code>PortType</code>,
     * with a {@link Replacer}. 
     *
     * @param name the name of this sendport.
     * @param r an object replacer, used in object serialization.
     * @param connectionAdministration set when this port must keep
     * connection administration to support the lostConnections and
     * newConnections downcalls.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public SendPort createSendPort(String name,
				   Replacer r,
				   boolean connectionAdministration)
	throws IOException;

    /**
     * Creates a anonymous {@link SendPort} of this <code>PortType</code>,
     * with a {@link Replacer}. 
     *
     * @param r an object replacer, used in object serialization.
     * @param cU object implementing the
     * {@link SendPortConnectUpcall#lostConnection(SendPort,
     * ReceivePortIdentifier, Exception)} method.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public SendPort createSendPort(Replacer r, SendPortConnectUpcall cU)
	throws IOException;

    /**
     * Creates a anonymous {@link SendPort} of this <code>PortType</code>,
     * with a {@link Replacer}. 
     *
     * @param name the name of this sendport.
     * @param r an object replacer, used in object serialization.
     * @param cU object implementing the
     * {@link SendPortConnectUpcall#lostConnection(SendPort,
     * ReceivePortIdentifier, Exception)} method.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public SendPort createSendPort(String name,
				   Replacer r,
				   SendPortConnectUpcall cU)
	throws IOException;


    /**
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with explicit receipt communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     * ConnectionAdministration is disabled.
     *
     * @param name the name of this receiveport.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public ReceivePort createReceivePort(String name) throws IOException;


    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with explicit receipt communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     *
     * @param name the name of this receiveport.
     * @param connectionAdministration set when this port must keep
     * connection administration to support the lostConnections and
     * newConnections downcalls.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public ReceivePort createReceivePort(String name,
					 boolean connectionAdministration)
	throws IOException;

    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with upcall based communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     * ConnectionAdministration is disabled.
     *
     * @param name the name of this receiveport.
     * @param u the upcall handler.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public ReceivePort createReceivePort(String name, Upcall u)
	throws IOException;

    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with upcall based communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     * ConnectionAdministration is disabled.
     *
     * @param name the name of this receiveport.
     * @param u the upcall handler.
     * @param connectionAdministration set when this port must keep
     * connection administration to support the lostConnections and
     * newConnections downcalls.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public ReceivePort createReceivePort(String name,
					 Upcall u,
					 boolean connectionAdministration)
	throws IOException;

    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with explicit receipt communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     * ConnectionAdministration is disabled.
     * When a new connection request arrives, or when a connection is lost,
     * a ConnectUpcall is performed.
     *
     * @param name the name of this receiveport.
     * @param cU object implementing <code>gotConnection</code>() and
     * <code>lostConnection</code>() upcalls.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public ReceivePort createReceivePort(String name,
					 ReceivePortConnectUpcall cU)
	throws IOException;

    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with upcall based communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     * ConnectionAdministration is disabled.
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
     */
    public ReceivePort createReceivePort(String name,
					 Upcall u,
					 ReceivePortConnectUpcall cU)
	throws IOException;

    /**
     * The hashCode method is mentioned here just as a reminder that an
     * implementation must probably redefine it, because two objects
     * representing the same porttype must result in the same hashcode
     * (and compare equal).
     * To explicitly specify it in the interface does not help,
     * because java.lang.Object already implements it,
     * but, anyway, here it is:
     */
    public int hashCode();

    /**
     * The equals method is mentioned here just as a reminder that an
     * implementation must probably redefine it, because two objects
     * representing the same porttype must compare equal (and result
     * in the same hashcode).
     * To explicitly specify it in the interface does not help,
     * because java.lang.Object already implements it,
     * but, anyway, here it is:
     *
     * @param other the object to compare with.
     */
    public boolean equals(Object other);
}
