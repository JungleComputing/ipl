/* $Id$ */

package ibis.ipl;

import java.io.IOException;
import java.util.Properties;

/**
 * A <code>PortType</code> represents a class of send and receive
 * ports with specific capabilities and with a specific role in the program.
 * Each class also
 * serves as the factory to create instances of these ports.
 * Ports can only connect to other ports of the same type.
 * A <code>PortType</code> is created using the
 * {@link Ibis#createPortType(CapabilitySet)} method. 
 * <p>
 * Support for connection downcalls can be explicitly turned on and off, because
 * it might incur some overhead. Moreover, if downcalls are used,
 * the amount of administration that must be kept is dependent on the
 * frequency of the user downcalls. If the user never does a downcall,
 * the administration is kept indefinitely.
 * </p>
 */

public interface PortType {

    /**
     * Returns the capabilities given to this PortType upon creation. 
     *
     * @return the capabilities of this port type.
     */
    public CapabilitySet capabilities();

    /**
     * Returns the properties given to this PortType upon creation.
     *
     * @return the properties of this port type.
     */
    public Properties properties();

    /**
     * Creates a anonymous {@link SendPort} of this <code>PortType</code>.
     * 
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    public SendPort createSendPort() throws IOException;

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
    public SendPort createSendPort(String name) throws IOException;

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
            throws IOException;

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
            throws IOException;

    /** 
     * Creates a named {@link SendPort} of this <code>PortType</code>.
     * The name does not have to be unique.
     * When a connection is lost, a ConnectUpcall is performed.
     *
     * @param name the name of this sendport.
     * @param cU object implementing the
     * {@link SendPortDisconnectUpcall#lostConnection(SendPort,
     * ReceivePortIdentifier, Throwable)} method.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public SendPort createSendPort(String name, SendPortDisconnectUpcall cU)
            throws IOException;

    /**
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with explicit receipt communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     *
     * @param name the unique name of this receiveport (or <code>null</code>,
     *    in which case the port is created anonymously).
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public ReceivePort createReceivePort(String name) throws IOException;

    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with explicit receipt communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     *
     * @param name the unique name of this receiveport (or <code>null</code>,
     *    in which case the port is created anonymously).
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
            boolean connectionDowncalls) throws IOException;

    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with upcall based communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     *
     * @param name the unique name of this receiveport (or <code>null</code>,
     *    in which case the port is created anonymously).
     * @param u the upcall handler.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public ReceivePort createReceivePort(String name, Upcall u)
            throws IOException;

    /** 
     * Creates a named {@link ReceivePort} of this <code>PortType</code>,
     * with upcall based communication.
     * New connections will not be accepted until
     * {@link ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     *
     * @param name the unique name of this receiveport (or <code>null</code>,
     *    in which case the port is created anonymously).
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
            boolean connectionDowncalls) throws IOException;

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
     *    in which case the port is created anonymously).
     * @param cU object implementing <code>gotConnection</code>() and
     * <code>lostConnection</code>() upcalls.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    public ReceivePort createReceivePort(String name,
            ReceivePortConnectUpcall cU) throws IOException;

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
     *    in which case the port is created anonymously).
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
            ReceivePortConnectUpcall cU) throws IOException;
}
