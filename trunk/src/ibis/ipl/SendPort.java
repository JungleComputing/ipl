/* $Id$ */

package ibis.ipl;

import java.io.IOException;
import java.util.Map;

/**
 * Maintains connections to one or more receive ports.
 *
 * When creating a sendport, it is possible to pass a
 * {@link SendPortDisconnectUpcall} object.
 * When a connection is lost for some reason (normal close or 
 * link error), the 
 * {@link SendPortDisconnectUpcall#lostConnection(SendPort,
 * ReceivePortIdentifier, Throwable)} upcall is invoked.
 * This upcall is completely asynchronous, but Ibis ensures that 
 * at most one is alive at any given time.
 *
 * When the port type has the ONE_TO_MANY capability set, no exceptions
 * are thrown by write methods in the write message.
 * Instead, the exception may be passed on to the <code>lostConnection</code>
 * upcall, in case a {@link SendPortDisconnectUpcall} is registered.
 * This allows the multicast to continue to the other destinations.
 * Ultimately, the {@link WriteMessage#finish() finish} method will
 * throw an exception.
 *
 * If no {@link SendPortDisconnectUpcall} is registered, the user is NOT
 * informed of connections that are lost.
 * If the port supports connection downcalls, the user can
 * use the {@link #lostConnections()} method to poll for connections 
 * that are lost.
 *
 * Connections are unrelated to messages! If the sending of a message 
 * did not generate an exception, this does not mean that it actually 
 * arrived at the receive port. There may still be data in Ibis or 
 * operating system buffers, or in the network itself. 
 *
 * For a given sendport, only one message is alive at any time.
 * This is done to prevent flow control problems.  When a
 * message is alive, and a new message is requested, the request is
 * blocked until the live message is finished.
 */
public interface SendPort extends Managable {

    /**
     * Returns the type that was used to create this port.
     * @return the <code>PortType</code>.
     */
    public PortType getType();

    /**
     * Requests a new message from this sendport.
     * It is allowed to get a message for a sendport that is not connected.
     * All data that is written into the message is then silently discarded.
     * When a message is alive, the request is blocked until the live
     * message is finished.
     *
     * @return a <code>WriteMessage</code>.
     * @exception IOException may be thrown when something goes wrong.
     */
    public WriteMessage newMessage() throws IOException;

    /**
     * Obtains an identification for this sendport.
     * @return the identification.
     */
    public SendPortIdentifier identifier();

    /**
     * Returns the name of the sendport.
     * When the sendport was created anonymously,
     * a system-invented name will be returned.
     *
     * @return the name.
     */
    public String name();

    /**
     * Returns the sum of the {@link ibis.ipl.WriteMessage#finish()} 
     * results for all write messages created with this port.
     * @return the number of bytes written.
     */
    public long getCount();

    /**
     * Sets the counter for the number of bytes that have been written to this
     * sendport to zero.
     */
    public void resetCount();

    /**
     * Attempts to set up a connection with a receiver.
     * It is not allowed to set up a new connection while a message
     * is alive.
     *
     * @param receiver identifies the {@link ReceivePort} to connect to
     * @exception ConnectionRefusedException is thrown
     * if the receiver denies the connection.
     * @exception AlreadyConnectedException is thrown if the port was already
     * connected to the receiver.
     * Multiple connections to the same receiver are NOT allowed.
     * @exception PortMismatchException is thrown if the receiveport
     * port and the sendport are of different types.
     * @exception IOException is thrown if a message is alive.
     */
    public void connect(ReceivePortIdentifier receiver) throws IOException;

    /**
     * Attempts to set up a connection with receiver.
     * It is not allowed to set up a new connection while a message
     * is alive.
     *
     * @param timeoutMillis timeout in milliseconds
     * @exception ibis.ipl.ConnectionTimedOutException is thrown
     * if an accept/deny has not arrived within <code>timeoutmillis</code>.
     * A value of 0 for <code>timeoutmillis</code> signifies no
     * timeout on the connection attempt.
     * @exception ConnectionRefusedException is thrown
     * if the receiver denies the connection.
     * @exception AlreadyConnectedException is thrown if the port was already
     * connected to the receiver.
     * Multiple connections to the same receiver are NOT allowed.
     * @exception PortMismatchException is thrown if the receiveport
     * port and the sendport are of different types.
     * @exception IOException is thrown if a message is alive.
     */
    public void connect(ReceivePortIdentifier receiver, long timeoutMillis)
            throws IOException;

    /**
     * Attempts to set up a connection with a receiver at the specified
     * Ibis instance, with the specified name.
     * It is not allowed to set up a new connection while a message
     * is alive.
     *
     * @param id identifies the Ibis instance on which the {@link ReceivePort}
     *   with the specified name is supposed to live.
     * @param name specifies the name of the {@link ReceivePort}.
     * @exception ConnectionRefusedException is thrown
     * if the receiver denies the connection.
     * @exception AlreadyConnectedException is thrown if the port was already
     * connected to the receiver.
     * Multiple connections to the same receiver are NOT allowed.
     * @exception PortMismatchException is thrown if the receiveport
     * port and the sendport are of different types.
     * @exception IOException is thrown if a message is alive.
     * @return the receiveport identifier.
     */
    public ReceivePortIdentifier connect(IbisIdentifier id, String name)
            throws IOException;

    /**
     * Attempts to set up a connection with a receiver at the specified
     * Ibis instance, with the specified name.
     * It is not allowed to set up a new connection while a message
     * is alive.
     *
     * @param id identifies the Ibis instance on which the {@link ReceivePort}
     *   with the specified name is supposed to live.
     * @param name specifies the name of the {@link ReceivePort}.
     * @param timeoutMillis timeout in milliseconds
     * @exception ibis.ipl.ConnectionTimedOutException is thrown
     * if an accept/deny has not arrived within <code>timeoutmillis</code>.
     * A value of 0 for <code>timeoutmillis</code> signifies no
     * timeout on the connection attempt.
     * @exception ConnectionRefusedException is thrown
     * if the receiver denies the connection.
     * @exception AlreadyConnectedException is thrown if the port was already
     * connected to the receiver.
     * Multiple connections to the same receiver are NOT allowed.
     * @exception PortMismatchException is thrown if the receiveport
     * port and the sendport are of different types.
     * @return the receiveport identifier.
     */
    public ReceivePortIdentifier connect(IbisIdentifier id, String name,
            long timeoutMillis) throws IOException;

    /**
     * Attempts to set up connections with the specified receivers.
     * It is not allowed to set up a new connection while a message
     * is alive.
     * @param ports the receivers.
     * @exception ConnectionsFailedException is thrown when one or more
     * connections fail. This exception object also identifies which connection
     * attempts actually succeeded.
     */
    public void connect(ibis.ipl.ReceivePortIdentifier[] ports)
            throws ConnectionsFailedException;

    /**
     * Attempts to set up connections with the specified receivers.
     * It is not allowed to set up a new connection while a message
     * is alive.
     * @param ports the receivers.
     * @param timeoutMillis timeout in milliseconds
     * @exception ConnectionsFailedException is thrown when one or more
     * connections fail. This exception object also identifies which connection
     * attempts actually succeeded.
     */
    public void connect(ibis.ipl.ReceivePortIdentifier[] ports,
            long timeoutMillis) throws ConnectionsFailedException;

    /**
     * Attempts to set up connection with receivers at the specified
     * Ibis instances, with the specified names.
     * It is not allowed to set up a new connection while a message
     * is alive.
     * @param ports maps ibis identifiers onto names. Together, an ibis
     * identifier and a name identify a receiver.
     * @exception ConnectionsFailedException is thrown when one or more
     * connections fail. This exception object also identifies which connection
     * attempts actually succeeded.
     * @return an array of receiveport identifiers.
     */
    public ibis.ipl.ReceivePortIdentifier[] connect(
            Map<ibis.ipl.IbisIdentifier, String> ports)
            throws ConnectionsFailedException;

    /**
     * Attempts to set up connection with receivers at the specified
     * Ibis instances, with the specified names.
     * It is not allowed to set up a new connection while a message
     * is alive.
     * @param ports maps ibis identifiers onto names. Together, an ibis
     * identifier and a name identify a receiver.
     * @param timeoutMillis timeout in milliseconds
     * @exception ConnectionsFailedException is thrown when one or more
     * connections fail. This exception object also identifies which connection
     * attempts actually succeeded.
     * @return an array of receiveport identifiers.
     */
    public ibis.ipl.ReceivePortIdentifier[] connect(
            Map<ibis.ipl.IbisIdentifier, String> ports, long timeoutMillis)
            throws ConnectionsFailedException;

    /**
     * Attempts to disconnect a connection with a receiver.
     *
     * @param receiver identifies the {@link ReceivePort} to disconnect
     * @exception IOException is thrown if there was no connection to
     * the receiveport specified or in case of other trouble.
     */
    public void disconnect(ReceivePortIdentifier receiver) throws IOException;

    /**
     * Frees the resources held by the sendport.
     * If a close is attempted when a message is still alive, an exception
     * will be thrown. Even if this call throws an exception, the sendport
     * cannot be used anymore.
     * @exception IOException is thrown in case of trouble.
     */
    public void close() throws IOException;

    /** 
     * Returns the set of receiveports this sendport is connected to.
     * When there are no connections, an array with 0 entries is returned.
     * 
     * @return an array of receiveport identifiers.
     */
    public ReceivePortIdentifier[] connectedTo();

    /** 
     * Polls to find out whether any connections are lost or closed.
     * Returns the changes since the last <code>lostConnections</code> call,
     * or, if this is the first call, all connections that were lost since
     * the port was created.
     * This call only works if the connectionDowncalls parameter was true
     * when this port was created.
     * If no connections were lost, an array with 0 entries is returned.
     * @return A set of receiveport identifiers to which the connection
     * is lost.
     * @exception IbisConfigurationException is thrown when connectionDowncalls
     * where not requested when creating the port.
     */
    public ReceivePortIdentifier[] lostConnections();
}
