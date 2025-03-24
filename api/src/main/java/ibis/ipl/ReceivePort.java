/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.ipl;

import java.io.IOException;

/**
 * A receiveport accepts and maintains connections from one or more sendports.
 * <p>
 * When creating a receiveport, it is possible to pass a
 * {@link ReceivePortConnectUpcall} object. Ibis will call the
 * {@link ReceivePortConnectUpcall#gotConnection(ReceivePort, SendPortIdentifier)}
 * upcall of this object when a sendport tries to initiate a new connection to
 * the receiveport. When a connection is lost for some reason (normal close or
 * link error), the
 * {@link ReceivePortConnectUpcall#lostConnection(ReceivePort, SendPortIdentifier, Throwable)}
 * upcall is performed. Both upcalls are completely asynchronous, but Ibis
 * ensures that at most one is active at any given time.
 * <p>
 * If no {@link ReceivePortConnectUpcall} is registered, the user is NOT
 * automatically informed of connections that are created or lost. If the port
 * supports connection downcalls, the user can use the
 * {@link #lostConnections()} method to poll for connections that are lost, and
 * the {@link #newConnections()} method to find out about new connections.
 * <p>
 * Only one upcall is alive at any one time, this includes BOTH message upcalls
 * AND ConnectUpcalls.
 * <p>
 * Only one message is alive at any time for a given receiveport. This is done
 * to prevent flow control problems. A receiveport can be configured to generate
 * upcalls or to support blocking receive, but NOT both! The message object is
 * always destroyed when it is finished.
 * <p>
 * The following {@link Manageable} items are recognized:
 * <TABLE>
 * <CAPTION>Manageable items</CAPTION> <TBODY>
 * <TR>
 * <TD>"Messages"
 * <TD>the number of messages received
 * <TR>
 * <TD>"MessageBytes"
 * <TD>the number of bytes received in messages
 * <TR>
 * <TD>"Bytes"
 * <TD>the total number of bytes received
 * <TR>
 * <TD>"Connections"
 * <TD>the total number of connections received with this port
 * <TR>
 * <TD>"LostConnections"
 * <TD>the number of lost connections
 * <TR>
 * <TD>"ClosedConnections"
 * <TD>the number of closed connections
 * <TR>
 * </TBODY>
 * </TABLE>
 * <p>
 * All these properties are long values, returned as a string.
 * <p>
 * Other items may be recognized, depending on the Ibis implementation.
 */
public interface ReceivePort extends Manageable {

    /**
     * Returns the type that was used to create this port.
     *
     * @return the type that was used to create this port.
     */
    public PortType getPortType();

    /**
     * Explicit blocking receive. This method blocks until a message arrives on this
     * receiveport. When a receiveport is configured to generate upcalls, using this
     * method is NOT allowed; in that case an {@link IbisConfigurationException} is
     * thrown.
     *
     * @return the message received.
     * @exception IbisConfigurationException is thrown when the receiveport is
     *                                       configured to use upcalls.
     * @exception IOException                is thrown in case of other trouble.
     */
    public ReadMessage receive() throws IOException;

    /**
     * Explicit blocking receive with timeout. This method blocks until a message
     * arrives on this receiveport, or the timeout expires. When an receiveport is
     * configured to generate upcalls, using this method is NOT allowed. The receive
     * blocks at most timeoutMillis, but it might be shorter! A timeoutMillis less
     * than or equal to 0 means just do a blocking receive.
     *
     * @param timeoutMillis timeout in milliseconds.
     * @return the message received.
     * @exception ReceiveTimedOutException   is thrown when the timeout expires and
     *                                       no message arrives.
     * @exception IbisConfigurationException is thrown when the receiveport is
     *                                       configured to use upcalls.
     * @exception IOException                is thrown in case of other trouble.
     **/
    public ReadMessage receive(long timeoutMillis) throws IOException;

    /**
     * Asynchronous explicit receive. Returns immediately, wether or not a message
     * is available. Also works for ports configured for upcalls, in which case it
     * is a normal poll: it will always return null, but it might generate an
     * upcall.
     *
     * @return the message received, or <code>null</code>.
     * @exception IbisConfigurationException is thrown when the receiveport is not
     *                                       configured to support polls.
     * @exception IOException                is thrown on IO errors.
     */
    public ReadMessage poll() throws IOException;

    /**
     * Returns the {@link ReceivePortIdentifier} of this receiveport.
     *
     * @return the identifier.
     */
    public ReceivePortIdentifier identifier();

    /**
     * Returns the name of the receiveport. When the receiveport was created
     * anonymously, a system-invented name will be returned.
     *
     * @return the name.
     */
    public String name();

    /**
     * Enables the accepting of new connections. When a receiveport is created it
     * will not accept connections, until this method is invoked. This is done to
     * avoid upcalls during initialization. After this method returns, connection
     * upcalls may be triggered.
     **/
    public void enableConnections();

    /**
     * Disables the accepting of new connections. It is allowed to invoke
     * {@link #enableConnections()} again after invoking this method. After this
     * method returns, no more connection upcalls will be given.
     */
    public void disableConnections();

    /**
     * Allows message upcalls to occur. This call is meaningless (and a no-op) for
     * receiveports that were created for explicit receive. Upon startup, message
     * upcalls are disabled. They must be explicitly enabled to receive message
     * upcalls.
     */
    public void enableMessageUpcalls();

    /**
     * Prohibits message upcalls. After this call, no message upcalls will occur
     * until {@link #enableMessageUpcalls()} is called. The
     * <code>disableMessageUpcalls</code>/<code>enableMessageUpcalls</code>
     * mechanism allows the user to selectively allow or disallow message upcalls
     * during runtime. <strong> Note: the
     * <code>disableMessageUpcalls</code>/<code>enableMessageUpcalls</code>
     * mechanism is not necessary to enforce serialization of Upcalls for this port.
     * </strong> Ibis already guarantees that only one message per port is active at
     * any time.
     */
    public void disableMessageUpcalls();

    /**
     * Frees the resources held by the receiveport. Important: this method blocks
     * until all sendports that are connected to it have been closed.
     *
     * @throws IOException in case of trouble.
     */
    public void close() throws IOException;

    /**
     * Frees the resources held by the receiveport, with timeout. Like
     * {@link #close()}, but blocks at most timeout milliseconds. When the close
     * does not succeed within the timeout, this operation does a forced close.
     * Important: this call does not block until all sendports that are connected to
     * it have been freed. Therefore, messages may be lost! Use this with extreme
     * caution! When this call is used, and this port is configured to maintain
     * connection administration, it updates the administration and thus may
     * generate lostConnection upcalls.
     *
     * @param timeoutMillis timeout in milliseconds. When zero, the call is
     *                      equivalent to a {@link #close()}; when less than zero,
     *                      the port is closed immediately.
     * @throws IOException in case of trouble.
     */
    public void close(long timeoutMillis) throws IOException;

    /**
     * Returns the set of sendports this receiveport is connected to. When there are
     * no connections, an array with 0 entries is returned.
     *
     * @return the sendport identifiers.
     */
    public SendPortIdentifier[] connectedTo();

    /**
     * Returns the connections that were lost. Connections can be lost due to an
     * error, or because the sender disconnected. Returns the changes since the last
     * lostConnections call, or, if this is the first call, all connections that
     * were lost since the port was created. This call only works if this port is
     * configured to maintain connection administration. If no connections were
     * lost, an array with 0 entries is returned.
     *
     * @return the lost connections.
     * @exception IbisConfigurationException is thrown when the port was not
     *                                       configured to support connection
     *                                       downcalls.
     */
    public SendPortIdentifier[] lostConnections();

    /**
     * Returns the new connections accepted by this receiveport. Returns the changes
     * since the last newConnections call, or, if this is the first call, all
     * connections that were created since the port was created. This call only
     * works if this port is configured to maintain connection administration. If
     * there are no new connections, an array with 0 entries is returned.
     *
     * @return the new connections.
     * @exception IbisConfigurationException is thrown when the port was not
     *                                       configured to support connection
     *                                       downcalls.
     */
    public SendPortIdentifier[] newConnections();
}
