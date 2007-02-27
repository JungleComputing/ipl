/* $Id$ */

package ibis.ipl;

import java.io.IOException;
import java.util.Map;

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
 * SendPortIdentifier, Throwable)}
 * upcall is performed. Both upcalls are completely
 * asynchronous, but Ibis ensures that at most one is active at any given
 * time.
 *
 * If no {@link ReceivePortConnectUpcall} is registered, the user is NOT
 * informed of connections that are created or lost.
 * If the port supports connection downcalls, the user can
 * use the {@link #lostConnections()} * method to poll for connections that
 * are lost,  and the {@link #newConnections()}  method to find out about new
 * connections.
 *
 * Only one upcall is alive at any one time, this includes BOTH
 * normal (message) upcalls AND ConnectUpcalls.
 *
 * Only one message is alive at any time for a given
 * receiveport. This is done to prevent flow control problems.
 * A receiveport can be configured to generate upcalls or to 
 * support blocking receive, but NOT both!  The message object
 * is always destroyed when it is finished.
 */
public interface ReceivePort {

    /**
     * Returns the type that was used to create this port.
     * @return the type that was used to create this port.
     */
    public PortType getType();
    
    /**
     * Explicit blocking receive.
     * This method blocks until a message arrives on this receiveport.
     * When a receiveport is configured to generate upcalls, 
     * using this method is NOT allowed; in that case an
     * {@link IbisConfigurationException} is thrown.
     *
     * @return the message received.
     * @exception IbisConfigurationException is thrown when the receiveport
     * is configured to use upcalls.
     * @exception IOException is thrown in case of other trouble.
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
     * @exception IbisConfigurationException is thrown when the receiveport
     * is configured to use upcalls.
     * @exception IOException is thrown in case of other trouble.
     **/
    public ReadMessage receive(long timeoutMillis) throws IOException;

    /**
     * Asynchronous explicit receive.
     * Returns immediately, wether or not a message is available. 
     * Also works for ports configured for upcalls, in which case it is a
     * normal poll: it will always return null, but it might generate an upcall.
     * @return the message received, or <code>null</code>.
     * @exception IbisConfigurationException is thrown when the receiveport
     * is not configured to support polls.
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
     * Returns the dynamic properties of
     * this port. The user can set some implementation-specific dynamic
     * properties of the port, by means of the
     * {@link ibis.ipl.ReceivePort#setProperty(String, Object)
     * setProperty} method.
     * @return the dynamic properties of this port.
     */
    public Map<String, Object> properties();

    /**
     * Sets a number of dynamic properties of
     * this port. The user can set some implementation-specific dynamic
     * properties of the port.
     * @param properties the dynamic properties to set.
     */
    public void setProperties(Map<String, Object> properties); 
    
    /**
     * Returns a dynamic property of
     * this port. The user can set some implementation-specific dynamic
     * properties of the port, by means of the
     * {@link ibis.ipl.ReceivePort#setProperty(String, Object)
     * setProperty} method.
     * @param key the key for the requested property.
     * @return the value associated with the property.
     */
    public Object getProperty(String key);
    
    /**
     * Sets a dynamic property of
     * this port. The user can set some implementation-specific dynamic
     * properties of the port.
     * @param key the key for the property.
     * @param val the value associated with the property.
     */
    public void setProperty(String key, Object val);

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
     * Frees the resources held by the receiveport, with timeout. 
     * Like {@link #close()}, but blocks at most timeout milliseconds.
     * When the close does not succeed within the timeout, this operation
     * does a forced close.
     * Important: this call does not block until all sendports that are
     * connected to it have been freed. Therefore, messages may be lost!
     * Use this with extreme caution!
     * When this call is used, and this port is configured to maintain
     * connection administration, it updates the administration and thus
     * may generate lostConnection upcalls.
     *
     * @param timeoutMillis timeout in milliseconds. When zero, the call is
     * equivalent to a {@link #close()}; when less than zero, the port is
     * closed immediately.
     */
    public void close(long timeoutMillis) throws IOException;

    /**
     * Returns the set of sendports this receiveport is connected to.
     * When there are no connections, an array with 0 entries is returned.
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
     * If no connections were lost, an array with 0 entries is returned.
     * @exception IbisConfigurationException is thrown when the port was
     * not configured to support connection downcalls.
     * @return the lost connections.
     */
    public SendPortIdentifier[] lostConnections();

    /**
     * Returns the new connections accepted by this receiveport.
     * Returns the changes since the last newConnections call,
     * or, if this is the first call, all connections that were created since
     * the port was created.
     * This call only works if this port is configured to maintain 
     * connection administration.
     * If there are no new connections, an array with 0 entries is returned.
     * @exception IbisConfigurationException is thrown when the port was
     * not configured to support connection downcalls.
     * @return the new connections.
     */
    public SendPortIdentifier[] newConnections();
}
