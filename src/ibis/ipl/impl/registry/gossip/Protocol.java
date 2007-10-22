package ibis.ipl.impl.registry.gossip;

final class Protocol {

    // opcodes

    public static final byte OPCODE_ARRG_GOSSIP = 1;

    public static final int NR_OF_OPCODES = 1;

    // replies

    static final byte REPLY_OK = 1;

    static final byte REPLY_ERROR = 2;

    // misc "options"

    public static final byte MAGIC_BYTE = 54;

    public static String opcodeString(byte opcode) {
        if (opcode == OPCODE_ARRG_GOSSIP) {
            return "ARRG_GOSSIP";
        } else {
            return "UNKNOWN";
        }
    }

}
