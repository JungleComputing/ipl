package ibis.impl.registry;

public class Protocol {

    // opcodes

    public static final byte OPCODE_JOIN = 1;

    public static final byte OPCODE_LEAVE = 2;

    public static final byte OPCODE_GOSSIP = 3;

    public static final byte OPCODE_ELECT = 4;

    public static final byte OPCODE_SEQUENCE_NR = 5;

    public static final byte OPCODE_DEAD = 6;

    public static final byte OPCODE_MAYBE_DEAD = 7;

    public static final byte OPCODE_SIGNAL = 8;

    public static final byte OPCODE_PING = 9;
    
    public static final byte OPCODE_PUSH = 10;

    public static final int NR_OF_OPCODES = 11;

    // replies

    public static final byte REPLY_OK = 1;

    public static final byte REPLY_ERROR = 2;

    // misc "options"

    public static final int MIN_EVENT_LIST_SEND_SIZE = 100;
    
    public static final int START_EVENT_LIST_SEND_SIZE = 1000;
    
    public static final int MAX_EVENT_LIST_SEND_SIZE = 10000;

    public static final int BOOTSTRAP_LIST_SIZE = 200;

    public static String opcodeString(byte opcode) {
        if (opcode == OPCODE_JOIN) {
            return "JOIN";
        } else if (opcode == OPCODE_LEAVE) {
            return "LEAVE";
        } else if (opcode == OPCODE_GOSSIP) {
            return "GOSSIP";
        } else if (opcode == OPCODE_ELECT) {
            return "ELECT";
        } else if (opcode == OPCODE_SEQUENCE_NR) {
            return "SEQUENCE_NR";
        } else if (opcode == OPCODE_DEAD) {
            return "DEAD";
        } else if (opcode == OPCODE_MAYBE_DEAD) {
            return "MAYBE_DEAD";
        } else if (opcode == OPCODE_SIGNAL) {
            return "SIGNAL";
        } else if (opcode == OPCODE_PING) {
            return "PING";
        } else if (opcode == OPCODE_PUSH) {
            return "PUSH";
        } else {
            return "UNKNOWN";
        }
    }

}
