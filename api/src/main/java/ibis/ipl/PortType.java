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
package ibis.ipl;

import java.io.DataInput;
import java.io.IOException;

/**
 * This class represents the type of a receive or send port. A PortType is a
 * capability set. The possible values in this set are included here. Note that
 * Ibis implementations may define additional capabilities.
 */
public final class PortType extends CapabilitySet {

    /** Prefix for connection capabilities. */
    final static String CONNECTION = "connection";

    /** Prefix for receive capabilities. */
    final static String RECEIVE = "receive";

    /** Prefix for serialization capabilities. */
    final static String SERIALIZATION = "serialization";

    /** Prefix for communication capabilities. */
    final static String COMMUNICATION = "communication";

    /**
     * This capability describes that a sendport can have at most one outgoing
     * connection, and a receiveport can have at most one incoming connection.
     */
    public final static String CONNECTION_ONE_TO_ONE = CONNECTION + ".onetoone";

    /**
     * This capability describes that a sendport can have more than one outgoing
     * connection, but receiveports can only have one incoming connection.
     */
    public final static String CONNECTION_ONE_TO_MANY = CONNECTION + ".onetomany";

    /**
     * This capability describes that a sendport can have at most one outgoing
     * connection, but receiveports can have multiple incoming connections.
     */
    public final static String CONNECTION_MANY_TO_ONE = CONNECTION + ".manytoone";

    /**
     * This capability describes that a sendport can have multiple outgoing
     * connections, and receiveports can have multiple incoming connections.
     */
    public final static String CONNECTION_MANY_TO_MANY = CONNECTION + ".manytomany";

    /** This capability describes that connection downcalls are supported. */
    public final static String CONNECTION_DOWNCALLS = CONNECTION + ".downcalls";

    /** This capability describes that connection upcalls are supported. */
    public final static String CONNECTION_UPCALLS = CONNECTION + ".upcalls";

    /**
     * This capability describes that timeouts on connection attempts are supported.
     */
    public final static String CONNECTION_TIMEOUT = CONNECTION + ".timeout";

    /**
     * This capability describes that lightweight connection setup is used.
     * Lightweight connections are cheaper to create and often use less kernel
     * resources. On the downside, they are likely to have a higher latency and
     * lower throughput.
     *
     * @ibis.experimental
     */
    public final static String CONNECTION_LIGHT = CONNECTION + ".light";

    /**
     * This capability describes that ultra-lightweight connection setup is used.
     * These connections are even cheaper to create and maintain than lightweight
     * connections. They are not reliable, however, and can not be combined with
     * connection up/downcalls. In addition, a maximum message size may be imposed.
     *
     * @ibis.experimental
     */
    public final static String CONNECTION_ULTRALIGHT = CONNECTION + ".ultralight";

    /**
     * This capability describes that -only- direct connection setup is used. This
     * prevents ibis from using any connection setup scheme that does not result in
     * a direct connection between the two machines. Note that using this option may
     * severely restrict the connectivity of an ibis instance.
     *
     * @ibis.experimental
     */
    public final static String CONNECTION_DIRECT = CONNECTION + ".direct";

    /** This capability describes that explicit receive is supported. */
    public final static String RECEIVE_EXPLICIT = RECEIVE + ".explicit";

    /**
     * This capability describes that explicit receive with a timeout is supported.
     */
    public final static String RECEIVE_TIMEOUT = RECEIVE + ".timeout";

    /** This capability describes that polls are supported. */
    public final static String RECEIVE_POLL = RECEIVE + ".poll";

    /**
     * This capability describes that message upcalls are supported without the need
     * to poll for them.
     */
    public final static String RECEIVE_AUTO_UPCALLS = RECEIVE + ".autoupcalls";

    /**
     * This capability describes that message upcalls are supported but the user
     * must to poll for them. An implementation that claims that it has this, may
     * also do autoupcalls, but polling does no harm. When an application asks for
     * this (and not autoupcalls), it must poll. For this to work, poll must be
     * supported and requested!
     */
    public final static String RECEIVE_POLL_UPCALLS = RECEIVE + ".pollupcalls";

    /**
     * This capability describes that messages from a sendport are delivered to the
     * receiveport(s) in the order in which they were sent.
     */
    public final static String COMMUNICATION_FIFO = COMMUNICATION + ".fifo";

    /**
     * This capability describes that messages are given global sequence numbers so
     * that the application can order them. The numbering is per send port name.
     */
    public final static String COMMUNICATION_NUMBERED = COMMUNICATION + ".numbered";

    /** This capability describes that communication is reliable. */
    public final static String COMMUNICATION_RELIABLE = COMMUNICATION + ".reliable";

    /** This capability describes that communication is unreliable. */
    public final static String COMMUNICATION_UNRELIABLE = COMMUNICATION + ".unreliable";

    /**
     * This capability describes that communication is reliable but not safe enough
     * to use on open networks.
     */
    public final static String COMMUNICATION_RELIABLE_UNSAFE = COMMUNICATION + ".reliable.unsafe";

    /**
     * This capability describes that readByte/writeByte and
     * readArray/writeArray(byte[]) are supported.
     */
    public final static String SERIALIZATION_BYTE = SERIALIZATION + ".byte";

    /**
     * This capability describes that read/write and readArray/writeArray of
     * primitive types are supported.
     */
    public final static String SERIALIZATION_DATA = SERIALIZATION + ".data";

    /**
     * This capability describes that some sort of object serialization is
     * supported. Applications may ask for a specific implementation by specifying,
     * for instance, serialization.object.sun.
     */
    public final static String SERIALIZATION_OBJECT = SERIALIZATION + ".object";

    /**
     * This capability describes that ibis object serialization is supported.
     */
    public final static String SERIALIZATION_OBJECT_IBIS = SERIALIZATION_OBJECT + ".ibis";

    /**
     * This capability describes that sun object serialization is supported.
     */
    public final static String SERIALIZATION_OBJECT_SUN = SERIALIZATION_OBJECT + ".sun";

    /**
     * Constructor for a port type.
     *
     * @param capabilities the capabilities of this port type.
     */
    public PortType(String... capabilities) {
        super(capabilities);
    }

    /**
     * Constructs a port type by reading it from the specified data input stream.
     *
     * @param dataInput the data input stream.
     * @throws IOException is thrown in case of trouble.
     */
    public PortType(DataInput dataInput) throws IOException {
        super(dataInput);
    }
}
