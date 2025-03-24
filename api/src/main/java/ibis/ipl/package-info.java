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
/**
 * This package describes the Ibis Portability Layer (IPL), which is to be
 * implemented by Ibis implementations. Ibis is a programming environment that
 * combines Java's "run everywhere" portability both with flexible treatment of
 * dynamically available networks and processor pools, and with highly
 * efficient, object-based communication.
 * <p>
 * Communication is based on {@link ibis.ipl.ReceivePort ReceivePorts} and
 * {@link ibis.ipl.SendPort SendPorts} of a certain port type, which is
 * represented by a {@link ibis.ipl.PortType PortType}. Port capabilities are
 * given to the port type (for example ports are "ONE_TO_ONE" and "RELIABLE" and
 * support "OBJECT_SERIALIZATION"). {@link ibis.ipl.ReceivePort ReceivePorts}
 * and {@link ibis.ipl.SendPort SendPorts} both have a port type. Only receive
 * ports and send ports of the same port type can communicate. Any number of
 * receive ports and send ports can be created on a JVM (even of the same port
 * type).
 * <p>
 * First, an {@link ibis.ipl.Ibis Ibis} instance must be created. This is
 * achieved by calling one of the <code>createIbis</code> methods of the
 * {@link ibis.ipl.IbisFactory IbisFactory}, specifying the
 * {@link ibis.ipl.IbisCapabilities capabilities} and port types required.
 * <p>
 * SendPorts and ReceivePorts are created by an Ibis instance using the
 * {@link ibis.ipl.Ibis#createSendPort(PortType, String) createSendPort} and
 * {@link ibis.ipl.Ibis#createReceivePort(PortType, String) createReceivePort}
 * methods. When creating a ReceivePort, it can be supplied with an
 * {@link ibis.ipl.MessageUpcall MessageUpcall} object. If so, upcalls are
 * generated when messages arrive. If not, explicit receive must be used to read
 * messages.
 * <p>
 * The system provides a globally unique {@link ibis.ipl.ReceivePortIdentifier
 * ReceivePortIdentifier} and {@link ibis.ipl.SendPortIdentifier
 * SendPortIdentifier} for every ReceivePort and SendPort. These identifiers are
 * implementation specific and serializable (and can be sent over the
 * network/saved in a file etc.).
 * <p>
 * A SendPort takes the initiative to connect to or disconnect from ReceivePorts
 * (otherwise the one-way traffic scheme is violated). A SendPort can connect to
 * one or more ReceivePorts using their ReceivePortIdentifiers or their Ibis
 * identifiers and names. Additional ReceivePorts may be connected to at any
 * time, provided that the port type allows it. A SendPort can disconnect from
 * one or more ReceivePorts using their ReceivePortIdentifiers. Additional
 * ReceivePorts may be disconnected at any time. When a SendPort is no longer
 * used it must be closed using the {@link ibis.ipl.SendPort#close() close}
 * method. When this method is called, all connections the SendPort has are
 * disconnected. When a ReceivePort is no longer used it must be closed using
 * the {@link ibis.ipl.ReceivePort#close() close} method. This call will block
 * until connections to SendPorts are disconnected (by the SendPorts).
 * <p>
 * A {@link ibis.ipl.WriteMessage message} can be sent from a SendPort to the
 * set of ReceivePorts it is connected to. To do this, a
 * {@link ibis.ipl.WriteMessage write message} is obtained from the SendPort
 * (this allows streaming, as the destination is known). Data can be added to
 * the message using "write" methods (this data may be immediately streamed to
 * the ReceivePorts) of this message. The Ibis system may or may not
 * asynchronously start sending the message. The message can be finished using
 * the {@link ibis.ipl.WriteMessage#finish() finish} method. When the finish
 * returns, all data has been copied (and now may be changed), and the message
 * may no longer be used.
 * <p>
 * When a {@link ibis.ipl.ReadMessage message} arrives at a ReceivePort, how it
 * is handled depends on the way the ReceivePort was instantiated. If messages
 * are to be handled by means of upcalls, a "new" thread is started and the
 * upcall is invoked with this message as parameter. When the message is no
 * longer used it MAY be returned to the system using the
 * {@link ibis.ipl.ReadMessage#finish() finish} method (after which the message
 * may no longer be used). If messages are to be handled by means of explicit
 * receive calls, a message is delivered when the
 * {@link ibis.ipl.ReceivePort#receive() receive} method is called. When the
 * message is no longer used it MUST be returned to the system using the
 * {@link ibis.ipl.ReadMessage#finish() finish} method (after which the message
 * may no longer be used). This allows the underlying implementation to deliver
 * a next message.
 *
 * All Ibis implementations must be thread-safe.
 */
package ibis.ipl;
