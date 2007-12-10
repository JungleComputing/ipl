package ibis.ipl.impl.registry.central;

public final class Protocol {
    
    public static final byte VERSION = 2;

    // opcodes

    public static final byte OPCODE_JOIN = 0;

    public static final byte OPCODE_LEAVE = 1;

    public static final byte OPCODE_GOSSIP = 2;

    public static final byte OPCODE_ELECT = 3;

    public static final byte OPCODE_SEQUENCE_NR = 4;

    public static final byte OPCODE_DEAD = 5;

    public static final byte OPCODE_MAYBE_DEAD = 6;

    public static final byte OPCODE_SIGNAL = 7;

    public static final byte OPCODE_PING = 8;

    public static final byte OPCODE_PUSH = 9;

    public static final byte OPCODE_BROADCAST = 10;

    public static final byte OPCODE_FORWARD = 11;
    
    public static final byte OPCODE_GET_STATE = 12;

    public static final byte OPCODE_HEARTBEAT = 13;

    public static final int NR_OF_OPCODES = 14;

    public static final String[] OPCODE_NAMES =
        { "JOIN", "LEAVE", "GOSSIP", "ELECT", "SEQUENCE_NR", "DEAD",
                "MAYBE_DEAD", "SIGNAL", "PING", "PUSH", "BCAST_PUSH", "GET_STATE",
                "HEARTBEAT", "STATISTICS" };

    static final int MIN_EVENT_LIST_SEND_SIZE = 100;

    static final int START_EVENT_LIST_SEND_SIZE = 1000;

    static final int MAX_EVENT_LIST_SEND_SIZE = 10000;

    public static final int BOOTSTRAP_LIST_SIZE = 200;

    public static final byte CLIENT_MAGIC_BYTE = 23;

    public static final byte SERVER_MAGIC_BYTE = 98;

}
