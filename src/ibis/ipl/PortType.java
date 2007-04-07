package ibis.ipl;

import java.io.DataInput;
import java.io.IOException;

/**
 * This class represents the type of a receive or send port,
 * the port type. It consists of a capability set and  some
 * predefined capability strings.
 */
public final class PortType extends CapabilitySet {
    
    /** Prefix for connection capabilities. */
    public final static String CONNECTION = "connection";

    /** Prefix for receive capabilities. */
    public final static String RECEIVE = "receive";

    /** Prefix for serialization capabilities. */
    public final static String SERIALIZATION = "serialization";

    /** Prefix for communication capabilities. */
    public final static String COMMUNICATION = "communication";
    /**
     * Boolean capability, set when a sendport can connect to a single
     * receiveport, that can accept at most one connection.
     */
    public final static String CONNECTION_ONE_TO_ONE = CONNECTION + ".onetoone";

    /**
     * Boolean capability, set when a sendport can connect to more than
     * one receiveport, which, in turn can accept at most one connection.
     */
    public final static String CONNECTION_ONE_TO_MANY
            = CONNECTION + ".onetomany";

    /**
     * Boolean capability, set when multiple sendports, which each can at most
     * have one outgoing connection, can connect to a single receiveport.
     */
    public final static String CONNECTION_MANY_TO_ONE
            = CONNECTION + ".manytoone";

    /**
     * Boolean capability, set when multiple sendports can connect to
     * multiple receiveports.
     */
    public final static String CONNECTION_MANY_TO_MANY
            = CONNECTION + ".manytomany";

    /** Boolean capability, set when connection downcalls are supported. */
    public final static String CONNECTION_DOWNCALLS = CONNECTION + ".downcalls";

    /** Boolean capability, set when connection upcalls are supported. */
    public final static String CONNECTION_UPCALLS = CONNECTION + ".upcalls";

    /**
     * Boolean capability, set when timeouts on connection attempts are
     * supported.
     */
    public final static String CONNECTION_TIMEOUT = CONNECTION + ".timeout";

    /** Boolean capability, set when explicit receive is supported. */
    public final static String RECEIVE_EXPLICIT = RECEIVE + ".explicit";

    /**
     * Boolean capability, set when explicit receive with a timeout
     * is supported.
     */
    public final static String RECEIVE_TIMEOUT = RECEIVE + ".timeout";

    /** Boolean capability, set when poll is supported. */
    public final static String RECEIVE_POLL = RECEIVE + ".poll";

    /**
     * Boolean capability, set when message upcalls are supported without
     * the need to poll for them.
     */
    public final static String RECEIVE_AUTO_UPCALLS = RECEIVE + ".autoupcalls";

    /**
     * Boolean capability, set when message upcalls are supported but the
     * user must to poll for them. An implementation that claims that it
     * has this, may also do autoupcalls, but polling does no harm.
     * When an application asks for this (and not autoupcalls), it must poll.
     * For this to work, poll must be supported and requested!
     */
    public final static String RECEIVE_POLL_UPCALLS = RECEIVE + ".pollupcalls";

    /**
     * Boolean capability, set when messages from a sendport are delivered
     * to the receiveport(s) in the order in which they were sent.
     */
    public final static String COMMUNICATION_FIFO = COMMUNICATION + ".fifo";

    /**
     * Messages from sendports of a port type with this property are
     * given global sequence numbers so that the application can order
     * them. The numbering is per port type.
     */
    public final static String COMMUNICATION_NUMBERED
            = COMMUNICATION + ".numbered";

    /** Boolean capability, set when reliable communication is supported. */
    public final static String COMMUNICATION_RELIABLE
            = COMMUNICATION + ".reliable";
    
    /**
     * Boolean capability, indicating that readByte/writeByte and
     * readArray/writeArray(byte[]) are supported.
     */
    public final static String SERIALIZATION_BYTE = SERIALIZATION + ".byte";

    /**
     * Boolean capability, indicating that read/write and readArray/writeArray
     * of primitive types are supported.
     */
    public final static String SERIALIZATION_DATA = SERIALIZATION + ".data";

    /**
     * Boolean capability, indicating that some sort of object serialization
     * is supported. Applications may ask for a specific implementation by
     * specifying, for instance, serialization.object.sun.
     */
    public final static String SERIALIZATION_OBJECT = SERIALIZATION + ".object";

    /**
     * String capability, indicating an object replacer for object
     * serialization.
     */
    public final static String SERIALIZATION_REPLACER
        = SERIALIZATION + ".replacer";

    /** 
     * Constructor for a port type.
     * @param capabilities the capabilities of this port type.
     */
    public PortType(String... capabilities) {
        super(capabilities);
    }

    /**
     * Constructs a port type by reading it from the specified byte array.
     * 
     * @param codedForm the byte array.
     * @param offset the offset at which to start reading.
     * @param length length to read.
     * @throws IOException is thrown on error.
     */
    public PortType(byte[] codedForm, int offset, int length) throws IOException {
        super(codedForm, offset, length);
    }

    /**
     * Constructs a port type by reading it from the specified byte array.
     * @param codedForm the byte array.
     * @throws IOException is thrown in case of error.
     */
    public PortType(byte[] codedForm) throws IOException {
        super(codedForm);
    }

//    /**
//     * Constructs a port type from the specified capabilityset.
//     * @param c the capabilityset.
//     */
//    public PortType(CapabilitySet c) {
//        super(c);
//    }

    /**
     * Constructs a port type by reading it from the specified data
     * input stream.
     * @param dis the data input stream.
     * @throws IOException is thrown in case of trouble.
     */
    public PortType(DataInput dis) throws IOException {
        super(dis);
    }

//    /**
//     * Constructs a port type from the specified properties.
//     * @param sp the properties.
//     */
//    protected PortType(Properties sp) {
//        super(sp);
//    }
}
