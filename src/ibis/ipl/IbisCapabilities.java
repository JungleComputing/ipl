package ibis.ipl;

/**
 * This interface specifies the predefined capabilities that Ibis
 * implementations may or may not implement. In addition, Ibis implementations
 * may add new ones, and Ibis applications may ask for them.
 * These capabilities are mentioned in a separate interface, so that an
 * application can import the names by implementing this interface, so that
 * it does not have to qualify every name.
 */
public interface IbisCapabilities {

    /** Prefix for connection capabilities. */
    final static String CONN_PREFIX = "connection.";

    /** Prefix for receive capabilities. */
    final static String RECV_PREFIX = "receive.";

    /** Prefix for serialization capabilities. */
    final static String SER_PREFIX = "serialization.";

    /** Prefix for communication capabilities. */
    final static String COMM_PREFIX = "communication.";

    /** Prefix for worldmodel capabilities. */
    final static String WORLD_PREFIX = "worldmodel.";

    /**
     * Boolean capability, set when a sendport can connect to more than
     * one receiveport.
     */
    public final static String CONN_ONETOMANY = CONN_PREFIX + "onetomany";

    /**
     * Boolean capability, set when multiple sendports can connect to a
     * single receiveport.
     */
    public final static String CONN_MANYTOONE = CONN_PREFIX + "manytoone";

    /** Boolean capability, set when connection downcalls are supported. */
    public final static String CONN_DOWNCALLS = CONN_PREFIX + "downcalls";

    /** Boolean capability, set when connection upcalls are supported. */
    public final static String CONN_UPCALLS = CONN_PREFIX + "upcalls";

    /**
     * Boolean capability, set when timeouts on connection attempts are
     * supported.
     */
    public final static String CONN_TIMEOUT = CONN_PREFIX + "timeout";

    /** Boolean capability, set when explicit receive is supported. */
    public final static String RECV_EXPLICIT = RECV_PREFIX + "explicit";

    /**
     * Boolean capability, set when explicit receive with a timeout
     * is supported.
     */
    public final static String RECV_TIMEOUT = RECV_PREFIX + "timeout";

    /** Boolean capability, set when poll is supported. */
    public final static String RECV_POLL = RECV_PREFIX + "poll";

    /**
     * Boolean capability, set when message upcalls are supported without
     * the need to poll for them.
     */
    public final static String RECV_AUTOUPCALLS = RECV_PREFIX + "autoupcalls";

    /**
     * Boolean capability, set when message upcalls are supported but the
     * user must to poll for them. An implementation that claims that it
     * has this, may also do autoupcalls, but polling does no harm.
     * When an application asks for this (and not autoupcalls), it must poll.
     * For this to work, poll must be supported and requested!
     */
    public final static String RECV_POLLUPCALLS = RECV_PREFIX + "pollupcalls";

    /**
     * Boolean capability, set when messages from a sendport are delivered
     * to the receiveport(s) in the order in which they were sent.
     */
    public final static String COMM_FIFO = COMM_PREFIX + "fifo";

    /**
     * Messages from sendports of a port type with this property are
     * given global sequence numbers so that the application can order
     * them. The numbering is per port type.
     */
    public final static String COMM_NUMBERED = COMM_PREFIX + "numbered";

    /** Boolean capability, set when reliable communication is supported. */
    public final static String COMM_RELIABLE = COMM_PREFIX + "reliable";

    /** Boolean capability, set when Ibises can join the run at any time. */
    public final static String WORLD_OPEN = WORLD_PREFIX + "open";

    /**
     * Boolean capability, set when the Ibises that can join the run are
     * determined at the start of the run.
     * Note that Ibis implementations may support both world models.
     */
    public final static String WORLD_CLOSED = WORLD_PREFIX + "closed";

    /**
     * Boolean capability, indicating that readByte/writeByte and
     * readArray/writeArray(byte[]) are supported.
     */
    public final static String SER_BYTE = SER_PREFIX + "byte";

    /**
     * Boolean capability, indicating that read/write and readArray/writeArray
     * of primitive types are supported.
     */
    public final static String SER_DATA = SER_PREFIX + "data";

    /**
     * Boolean capability, indicating that some sort of object serialization
     * is supported. This is sufficient if all writeObject/readObject methods
     * are symmetrical, t.i. readObject reads everything that writeObject
     * writes, and no versioning support is needed (object versions are the
     * same on both ends).
     */
    public final static String SER_OBJECT = SER_PREFIX + "object";

    /**
     * Boolean capability, indicating that object serialization, exactly
     * as specified by SUN is supported.
     */
    public final static String SER_STRICTOBJECT = SER_PREFIX + "strictobject";

    /**
     * String capability, indicating a nickname (or just the classname)
     * for a specific Ibis implementation. An Ibis implementation can
     * specify a nickname in its "capabilities" file, and an application
     * can request a specific Ibis implementation by adding it to
     * the requested capabilities.
     */
    public final static String NICKNAME = "nickname";
}

