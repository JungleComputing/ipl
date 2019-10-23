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
import java.util.Map;

/**
 * Initiates and maintains connections to one or more receive ports.
 * <p>
 * When creating a sendport, it is possible to pass a
 * {@link SendPortDisconnectUpcall} object. When a connection is lost for some
 * reason, the
 * {@link SendPortDisconnectUpcall#lostConnection(SendPort, ReceivePortIdentifier, Throwable)}
 * upcall is invoked. This upcall is completely asynchronous, but Ibis ensures
 * that at most one is alive at any given time. A receiveport can forcibly close
 * the connection, in which case any communication from the sendport will cause
 * a lostConnection upcall. <strong> The user may not assume that the mere fact
 * that a receive port forcibly closes its connections causes a lostConnection
 * call on the send port side. The send port has to do communication to detect
 * that there is trouble. </strong>
 * <p>
 * If no {@link SendPortDisconnectUpcall} is registered, the user is NOT
 * automatically informed of connections that are lost. If the port supports
 * connection downcalls, the user can use the {@link #lostConnections()} method
 * to poll for connections that are lost. Again, the send port has to do
 * communication to detect that there may be trouble.
 * <p>
 * Connections are unrelated to messages! If the sending of a message did not
 * generate an exception, this does not mean that it actually arrived at the
 * receive port. There may still be data in Ibis or operating system buffers, or
 * in the network itself.
 * <p>
 * For a given sendport, only one message is alive at any time. This is done to
 * prevent flow control problems. When a message is alive, and a new message is
 * requested, the request blocks until the live message is finished.
 * <p>
 * The following {@link Manageable} items are recognized:
 * <TABLE>
 * <CAPTION>Manageable items</CAPTION> <TBODY>
 * <TR>
 * <TD>"Messages"
 * <TD>the number of messages sent
 * <TR>
 * <TD>"MessageBytes"
 * <TD>the number of bytes sent in messages (multicasts are counted once)
 * <TR>
 * <TD>"Bytes"
 * <TD>the total number of bytes sent
 * <TR>
 * <TD>"Connections"
 * <TD>the total number of connections made with this port
 * <TR>
 * <TD>"LostConnections"
 * <TD>the number of lost connections
 * <TR>
 * <TD>"ClosedConnections"
 * <TD>the number of closed or disconnected connections
 * <TR>
 * </TBODY>
 * </TABLE>
 * <p>
 * All these properties are long values, returned as a string.
 * <p>
 * Other items may be recognized, depending on the Ibis implementation.
 */
public interface SendPort extends Manageable {

    /**
     * Returns the type that was used to create this port.
     *
     * @return the port type.
     */
    public PortType getPortType();

    /**
     * Requests a new message from this sendport. It is allowed to get a message
     * for a sendport that is not connected. All data that is written into the
     * message is then silently discarded. When a message is alive, the request
     * blocks until the live message is finished.
     *
     * @return a <code>WriteMessage</code>.
     * @exception IOException
     *                may be thrown when something goes wrong.
     */
    public WriteMessage newMessage() throws IOException;

    /**
     * Obtains an identification for this sendport.
     *
     * @return the identification.
     */
    public SendPortIdentifier identifier();

    /**
     * Returns the name of the sendport. When the sendport was created
     * anonymously, a system-invented name will be returned.
     *
     * @return the name.
     */
    public String name();

    /**
     * Attempts to set up a connection with a receiver. It is not allowed to set
     * up a new connection while a message is alive.
     *
     * @param receiver
     *            identifies the {@link ReceivePort} to connect to
     * @exception ConnectionRefusedException
     *                is thrown if the receiver denies the connection.
     * @exception AlreadyConnectedException
     *                is thrown if the port was already connected to the
     *                receiver. Multiple connections to the same receiver are
     *                NOT allowed.
     * @exception PortMismatchException
     *                is thrown if the receiveport port and the sendport are of
     *                different types.
     * @exception ConnectionFailedException
     *                is thrown in case of trouble.
     */
    public void connect(ReceivePortIdentifier receiver)
            throws ConnectionFailedException;

    /**
     * Attempts to set up a connection with receiver. This method is guarenteed
     * not to take longer than the specified timeout. When
     * <code>fillTimeout</code> is set to true, an unsuccessful connection setup
     * will be retried automatically until a connection is established, or the
     * timeout expires. It is not allowed to set up a new connection while a
     * message is alive.
     *
     * @param receiver
     *            identifies the {@link ReceivePort} to connect to.
     * @param timeoutMillis
     *            timeout in milliseconds
     * @param fillTimeout
     *            when set, unsuccessful connection setup will be retried until
     *            a connection is established or the timeout expires.
     * @exception ConnectionTimedOutException
     *                is thrown if an accept/deny has not arrived within
     *                <code>timeoutmillis</code>. A value of 0 for
     *                <code>timeoutmillis</code> signifies no timeout on the
     *                connection attempt.
     * @exception ConnectionRefusedException
     *                is thrown if the receiver denies the connection.
     * @exception AlreadyConnectedException
     *                is thrown if the port was already connected to the
     *                receiver. Multiple connections to the same receiver are
     *                NOT allowed.
     * @exception PortMismatchException
     *                is thrown if the receiveport port and the sendport are of
     *                different types.
     * @exception ConnectionFailedException
     *                is thrown in case of trouble.
     */
    public void connect(ReceivePortIdentifier receiver, long timeoutMillis,
            boolean fillTimeout) throws ConnectionFailedException;

    /**
     * Attempts to set up a connection with a receiver at the specified Ibis
     * instance, with the specified name. It is not allowed to set up a new
     * connection while a message is alive.
     *
     * @return the receiveport identifier.
     * @param ibisIdentifier
     *            identifies the Ibis instance on which the {@link ReceivePort}
     *            with the specified name is supposed to live.
     * @param receivePortName
     *            specifies the name of the {@link ReceivePort}.
     * @exception ConnectionRefusedException
     *                is thrown if the receiver denies the connection.
     * @exception AlreadyConnectedException
     *                is thrown if the port was already connected to the
     *                receiver. Multiple connections to the same receiver are
     *                NOT allowed.
     * @exception PortMismatchException
     *                is thrown if the receiveport port and the sendport are of
     *                different types.
     * @exception ConnectionFailedException
     *                is thrown in case of trouble.
     */
    public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier,
            String receivePortName) throws ConnectionFailedException;

    /**
     * Attempts to set up a connection with a receiver at the specified Ibis
     * instance, with the specified name. This method is guarenteed not to take
     * longer than the specified timeout. When fillTimeout is set to true, an
     * unsuccessful connection setup will be retried automatically until a
     * connection is established or the timeout expires. It is not allowed to
     * set up a new connection while a message is alive.
     *
     * @return the receiveport identifier.
     * @param ibisIdentifier
     *            identifies the Ibis instance on which the {@link ReceivePort}
     *            with the specified name is supposed to live.
     * @param receivePortName
     *            specifies the name of the {@link ReceivePort}.
     * @param timeoutMillis
     *            timeout in milliseconds
     * @param fillTimeout
     *            whether an unsuccessful connection setup should be retried
     *            automatically until the timeout expires
     * @exception ConnectionTimedOutException
     *                is thrown if an accept/deny has not arrived within
     *                <code>timeoutmillis</code>. A value of 0 for
     *                <code>timeoutmillis</code> signifies no timeout on the
     *                connection attempt.
     * @exception ConnectionRefusedException
     *                is thrown if the receiver denies the connection.
     * @exception AlreadyConnectedException
     *                is thrown if the port was already connected to the
     *                receiver. Multiple connections to the same receiver are
     *                NOT allowed.
     * @exception PortMismatchException
     *                is thrown if the receiveport port and the sendport are of
     *                different types.
     * @exception ConnectionFailedException
     *                in case of trouble.
     */
    public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier,
            String receivePortName, long timeoutMillis, boolean fillTimeout)
            throws ConnectionFailedException;

    /**
     * Attempts to set up connections with the specified receivers. It is not
     * allowed to set up a new connection while a message is alive.
     *
     * @param receivePortIdentifiers
     *            the receivers.
     * @exception ConnectionsFailedException
     *                is thrown when one or more connections fail. This
     *                exception object also identifies which connection attempts
     *                actually succeeded.
     */
    public void connect(ibis.ipl.ReceivePortIdentifier[] receivePortIdentifiers)
            throws ConnectionsFailedException;

    /**
     * Attempts to set up connections with the specified receivers. It is not
     * allowed to set up a new connection while a message is alive. This method
     * is guarenteed not to take longer than the specified timeout. When
     * fillTimeout is set to true, unsuccessful connection setups will be
     * retried automatically until all connections are established or the
     * timeout expires.
     *
     * @param receivePortIdentifiers
     *            the receivers.
     * @param timeoutMillis
     *            timeout in milliseconds
     * @param fillTimeout
     *            retry connection setup while there is time left
     * @exception ConnectionsFailedException
     *                is thrown when one or more connections fail. This
     *                exception object also identifies which connection attempts
     *                actually succeeded.
     */
    public void connect(ibis.ipl.ReceivePortIdentifier[] receivePortIdentifiers,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException;

    /**
     * Attempts to set up connection with receivers at the specified Ibis
     * instances, with the specified names. It is not allowed to set up a new
     * connection while a message is alive.
     *
     * @param ports
     *            maps ibis identifiers onto names. Together, an ibis identifier
     *            and a name identify a receiver.
     * @exception ConnectionsFailedException
     *                is thrown when one or more connections fail. This
     *                exception object also identifies which connection attempts
     *                actually succeeded.
     * @return an array of receiveport identifiers.
     */
    public ibis.ipl.ReceivePortIdentifier[] connect(
            Map<ibis.ipl.IbisIdentifier, String> ports)
            throws ConnectionsFailedException;

    /**
     * Attempts to set up connection with receivers at the specified Ibis
     * instances, with the specified names. This method is guarenteed not to
     * take longer than the specified timeout. When fillTimeout is set to true,
     * unsuccessful connection setups will be retried automatically until all
     * connections are established or the timeout expires. It is not allowed to
     * set up a new connection while a message is alive.
     *
     * @param ports
     *            maps ibis identifiers onto names. Together, an ibis identifier
     *            and a name identify a receiver.
     * @param timeoutMillis
     *            timeout in milliseconds
     * @param fillTimeout
     *            whether unsuccessful connection setups should be retried
     *            automatically until the timeout expires
     * @return an array of receiveport identifiers.
     * @exception ConnectionsFailedException
     *                is thrown when one or more connections fail. This
     *                exception object also identifies which connection attempts
     *                actually succeeded.
     */
    public ibis.ipl.ReceivePortIdentifier[] connect(
            Map<ibis.ipl.IbisIdentifier, String> ports, long timeoutMillis,
            boolean fillTimeout) throws ConnectionsFailedException;

    /**
     * Attempts to disconnect a connection with a receiver.
     *
     * @param receiver
     *            identifies the {@link ReceivePort} to disconnect
     * @exception IOException
     *                is thrown if there was no connection to the receiveport
     *                specified or in case of other trouble.
     */
    public void disconnect(ReceivePortIdentifier receiver) throws IOException;

    /**
     * Attempts to disconnect a connection with a receiver at the specified Ibis
     * instance, with the specified name.
     *
     * @param ibisIdentifier
     *            identifies the Ibis instance on which the {@link ReceivePort}
     *            with the specified name is supposed to live.
     * @param receivePortName
     *            specifies the name of the {@link ReceivePort}.
     * @exception IOException
     *                is thrown if there was no connection to the receiveport
     *                specified or in case of other trouble.
     */
    public void disconnect(IbisIdentifier ibisIdentifier,
            String receivePortName) throws IOException;

    /**
     * Frees the resources held by the sendport. If a close is attempted when a
     * message is still alive, an exception will be thrown. Even if this call
     * throws an exception, the sendport cannot be used anymore.
     *
     * @exception IOException
     *                is thrown in case of trouble.
     */
    public void close() throws IOException;

    /**
     * Returns the set of receiveports this sendport is connected to. When there
     * are no connections, an array with 0 entries is returned.
     *
     * @return an array of receiveport identifiers.
     */
    public ReceivePortIdentifier[] connectedTo();

    /**
     * Polls to find out whether any connections are lost or closed. Returns the
     * changes since the last <code>lostConnections</code> call, or, if this is
     * the first call, all connections that were lost since the port was
     * created. This call only works if the connectionDowncalls parameter was
     * true when this port was created. If no connections were lost, an array
     * with 0 entries is returned.
     *
     * @return a set of receiveport identifiers to which the connection is lost.
     * @exception IbisConfigurationException
     *                is thrown when the port type does not specify the
     *                connection downcalls capability.
     */
    public ReceivePortIdentifier[] lostConnections();
}
