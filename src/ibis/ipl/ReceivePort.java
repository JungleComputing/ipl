package ibis.ipl;

import java.io.IOException;

/**
 * A receiveport maintains connections to one or more sendports.
 *
 * When creating a receiveport, it is possible to pass a
 * {@link ReceivePortConnectUpcall} object. Ibis will call the
 * {@link ReceivePortConnectUpcall#gotConnection(ReceivePort,
 * SendPortIdentifier)} upcall
 * of this object when a sendport tries to initiate a new 
 * connection to the receiveport.  When a connection is lost 
 * for some reason (normal close or link error), the 
 * {@link ReceivePortConnectUpcall#lostConnection(ReceivePort,
 * SendPortIdentifier, Exception)}
 * upcall is performed. Both upcalls are completely
 * asynchronous, but Ibis ensures that at most one is active at any given
 * time.
 *
 * If no {@link ReceivePortConnectUpcall} is registered, the user is NOT
 * informed of connections that are created or lost.
 * No exceptions are thrown by the read message when a connection is lost.
 * It is the user's own responsibility to use the {@link #lostConnections()}
 * method to poll for connections that are lost.  The {@link #newConnections()}
 * method can be used to find out about new connections.
 *
 * Only one upcall is alive at a one time, this includes BOTH
 * normal (message) upcalls AND ConnectUpcalls.
 *
 * Only one message is alive at one time for a given
 * receiveport. This is done to prevent flow control problems.
 * A receiveport can be configured to generate upcalls or to 
 * support blocking receive, but NOT both!  The message object
 * is always destroyed when the upcall is finished; it is thus 
 * not correct to put it in a global variable / queue.
 */
public interface ReceivePort { 

    /**
     * Explicit blocking receive.
     * This method blocks until a message arrives on this receiveport.
     * When a receiveport is configured to generate upcalls, 
     * using this method is NOT allowed; in that case an {@link IOException}
     * is thrown.
     *
     * @return the message received.
     * @exception IOException is thrown when the receiveport is configured
     * to use upcalls, or something else is wrong.
     */
    public ReadMessage receive() throws IOException;

    /** 
     * Explicit blocking receive with timeout.
     * This method blocks until a message arrives on this receiveport, or
     * the timeout expires.
     * When an receiveport is configured to generate upcalls,
     * using this method is NOT allowed.
     * The receive blocks at most timeoutMillis, but it might be shorter!
     * A timeoutMillis less than or equal to 0 means just do a blocking receive.
     *
     * @param timeoutMillis timeout in milliseconds.
     * @return the message received.
     * @exception ReceiveTimedOutException is thrown when the timeout
     * expires and no message arrives.
     * @exception IOException is thrown when the receiveport is configured
     * to use upcalls, or something else is wrong.
     **/
    public ReadMessage receive(long timeoutMillis) throws IOException;

    /**
     * Asynchronous explicit receive.
     * Returns immediately, wether or not a message is available. 
     * Also works for ports configured for upcalls, in which case it is a
     * normal poll: it will always return null, but it might generate an upcall.
     * @return the message received, or <code>null</code>.
     * @exception IOException on IO error.
     */
    public ReadMessage poll() throws IOException;

    /**
     * Returns the number of bytes read from this receiveport.
     * A receiveport maintains a counter of how many bytes are used for
     * messages.
     * Each time a message is being finished, this counter is updated.
     *
     * @return the number of bytes read.
     **/
    public long getCount();

    /**
     * Resets the counter for the number of bytes read from this receive port
     * to zero.
     */
    public void resetCount();

    /**
     * Returns the {@link ibis.ipl.DynamicProperties DynamicProperties} of
     * this port.
     * The user can set some implementation-specific dynamic properties of the
     * port, by means of the
     * {@link ibis.ipl.DynamicProperties#set(String, Object)
     * DynamicProperties.set} method.
     */
    public DynamicProperties properties();

    /**
     * Returns the {@link ReceivePortIdentifier} of this receiveport.
     * @return the identifier.
     */
    public ReceivePortIdentifier identifier();

    /**
     * Returns the name of the receiveport.
     * When the receiveport was created anonymously, a system-invented
     * name will be returned.
     *
     * @return the name.
     */
    public String name();

    /**
     * Enables the accepting of new connections.
     * When a receiveport is created it will not accept connections, 
     * until this method is invoked. This is done to avoid upcalls
     * during initialization.
     * After this method returns, connection upcalls may be triggered.
     **/
    public void enableConnections();

    /**
     * Disables the accepting of new connections.
     * It is allowed to invoke {@link #enableConnections()} again
     * after invoking this method.
     * After this method returns, no more connection upcalls will be given.
     */
    public void disableConnections();

    /**
     * Allows message upcalls to occur.
     * This call is meaningless (and a no-op) for receiveports that were
     * created for explicit receive.
     * Upon startup, message upcalls are disabled.
     * They must be explicitly enabled to receive message upcalls.
     */
    public void enableUpcalls();

    /**
     * Prohibits message upcalls.
     * After this call, no message upcalls will occur until
     * {@link #enableUpcalls()} is called.
     * The <code>disableUpcalls</code>/<code>enableUpcalls</code> mechanism
     * allows the user to selectively allow or disallow message upcalls during
     * program run.
     * <strong>Note: the
     * <code>disableUpcalls</code>/<code>enableUpcalls</code>
     * mechanism is not necessary to enforce serialization of Upcalls for
     * this port.</strong> Ibis already guarantees that only one message
     * per port is active at any time.
     */
    public void disableUpcalls();

    /** 
     * Frees the resources held by the receiveport. 
     * Important: this method blocks until all sendports that are
     * connected to it have been closed. 
     */
    public void close() throws IOException;

    /** 
     * Frees the resources held by the receiveport. 
     * Important: this call does not block until all sendports that are
     * connected to it have been freed. Therefore, messages may be lost!
     * Use this with extreme caution!
     * When this call is used, and this port is configured to maintain
     * connection administration, it updates the administration and thus
     * may generate lostConnection upcalls.
     */
    public void forcedClose() throws IOException;

    /**
     * Frees the resources held by the receiveport, with timeout. 
     * Like {@link #close()}, but blocks at most timeout milliseconds.
     * When the free does not succeed within the timeout, this operation
     * does a {@link #forcedClose()}.
     * When this call is used, and this port is configured to maintain
     * connection administration, it updates the administration and thus
     * may generate lostConnection upcalls.
     * @param timeoutMillis timeout in milliseconds.
     */
    public void forcedClose(long timeoutMillis) throws IOException;

    /**
     * Returns the set of sendports this receiveport is connected to .
     * @return the sendport identifiers.
     */
    public SendPortIdentifier[] connectedTo();

    /**
     * Returns the connections that were lost.
     * Connections can be lost due to an error, or because the sender
     * disconnected. 
     * Returns the changes since the last lostConnections call,
     * or, if this is the first call, all connections that were lost since
     * the port was created.
     * This call only works if this port is configured to maintain
     * connection administration.
     * If not, <code>null</code> is returned.
     * @return the lost connections.
     */
    public SendPortIdentifier[] lostConnections();

    /**
     * Returns the new connections accepted by this receiveport.
     * Returns the changes since the last newConnections call,
     * or, if this is the first call, all connectcions that were created since
     * the port was created.
     * This call only works if this port is configured to maintain 
     * connection administration.
     * If not, <code>null</code> is returned.
     * @return the new connections.
     */
    public SendPortIdentifier[] newConnections();
} 
