package ibis.ipl;

/**
 * This interface specifies the predefined capabilities that Ibis
 * implementations may or may not implement. In addition, Ibis implementations
 * may add new ones, and Ibis applications may ask for them.
 */
public interface PredefinedCapabilities {

    /** Prefix for connection capabilities. */
    public final static String CONNECTION = "connection";

    /** Prefix for receive capabilities. */
    public final static String RECEIVE = "receive";

    /** Prefix for serialization capabilities. */
    public final static String SERIALIZATION = "serialization";

    /** Prefix for communication capabilities. */
    public final static String COMMUNICATION = "communication";

    /** Prefix for worldmodel capabilities. */
    public final static String WORLDMODEL = "worldmodel";

    /** Prefix for resize capabilities. */
    final static String RESIZE = "resize";

    /** Prefix for registry capabilities. */
    final static String REGISTRY = "registry";

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
     * Boolean capability, set when the Ibises that can join the run are
     * determined at the start of the run. This enables the methods
     * {@link Ibis#totalNrOfIbisesInPool()} and {@link Ibis#waitForAll()}.
     */
    public final static String WORLDMODEL_CLOSED = WORLDMODEL + ".closed";

    public final static String WORLDMODEL_OPEN = WORLDMODEL + ".open";

    
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

    /** Boolean capability, indicating that resize downcalls are supported. */
    public final static String RESIZE_DOWNCALLS = RESIZE + ".downcalls";

    /**
     * Boolean capability, indicating that registry event handlers are
     * supported.
     */
    public final static String REGISTRY_EVENTS = REGISTRY + ".events";
}
