package ibis.ipl;

import java.io.IOException;

/**
 * Maintains connections to one or more receive ports.
 *
 * When creating a sendport, it is possible to pass a
 * {@link SendPortConnectUpcall} object.
 * When a connection is lost for some reason (normal close or 
 * link error), the 
 * {@link SendPortConnectUpcall#lostConnection(SendPort,
 * ReceivePortIdentifier, Exception)} upcall is invoked.
 * This upcall is completely asynchronous, but Ibis ensures that 
 * at most one is alive at any given time.
 *
 * If no {@link SendPortConnectUpcall} is registered, the user is NOT informed 
 * of connections that are lost. No exceptions are thrown by 
 * the write message. It is then the user's own responisbility 
 * to use the {@link #lostConnections()} method to poll for connections 
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
public interface SendPort
{
    /**
     * Requests a new message from this sendport.
     * It is allowed to get a message for a sendport that is not connected.
     * All data that is written into the message is then silently discarded.
     *
     * @return a <code>WriteMessage</code>.
     * @exception IOException may be thrown when something goes wrong.
     */
    public WriteMessage newMessage () throws IOException;

    /**
     * Installs an object replacer on the underlying object stream.
     * This can be used, for instance, by an RMI implementation to
     * replace remote objects by stubs.
     * @param r the object replacer.
     */
    public void setReplacer(Replacer r);

    /**
     * Returns the {@link ibis.ipl.DynamicProperties DynamicProperties} of
     * this port. The user can set some implementation-specific dynamic
     * properties of the port, by means of the
     * {@link ibis.ipl.DynamicProperties#set(String, Object)
     * DynamicProperties.set} method.
     */
    public DynamicProperties properties ();

    /**
     * Obtains an identification for this sendport.
     * @return the identification.
     */
    public SendPortIdentifier identifier ();

    /**
     * Returns the name of the sendport.
     * When the sendport was created anonymously,
     * a system-invented name will be returned.
     *
     * @return the name.
     */
    public String name ();

    /**
     * Returns the number of bytes that have been written to this sendport.
     * @return the number of bytes written.
     */
    public long getCount ();

    /**
     * Sets the counter for the number of bytes that have been written to this
     * sendport to zero.
     */
    public void resetCount ();

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
     * Attempts to disconnect a connection with a receiver.
     *
     * @param receiver identifies the {@link ReceivePort} to disconnect
     * @exception IOException is thrown if there was no connection to
     * the receiveport specified or in case of other trouble.
     */
    public void disconnect (ReceivePortIdentifier receiver) throws IOException;

    /**
     * Attempts to set up a connection with receiver.
     *
     * @param receiver identifies the {@link ReceivePort} to connect to
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
     */
    public void connect (ReceivePortIdentifier receiver,
			 long timeoutMillis) throws IOException;

    /**
     * Frees the resources held by the sendport.
     * If a close is attempted when a message is still alive, an exception
     * will be thrown. Even if this call throws an exception, the connection
     * cannot be used anymore.
     * @exception IOException is thrown in case of trouble.
     */
    public void close () throws IOException;

    /** 
     * Returns the set of receiveports this sendport is connected to.
     * 
     * @return a set of receiveport identifiers.
     */
    public ReceivePortIdentifier[] connectedTo ();

    /** 
     * Polls to find out whether any connections are lost or closed.
     * Returns the changes since the last <code>lostConnections</code> call,
     * or, if this is the first call, all connections that were lost since
     * the port was created.
     * This call only works if the connectionAdministration parameter was true
     * when this port was created. Otherwise, null is returned.
     * @return A set of receiveport identifiers to which the connection
     * is lost.
     */
    public ReceivePortIdentifier[] lostConnections ();
}
