package ibis.gmi;

/**
 * Constants, placed in an interface to allow for easy access.
 */
public interface GroupProtocol {

    /* ========== Major opcodes ========== */

    /** Major opcode for a request to the registry. */
    public static final byte REGISTRY = 0;

    /** Major opcode for an answer of the registry. */
    public static final byte REGISTRY_REPLY = 1;

    /** Major opcode for an invocation. */
    public static final byte INVOCATION = 2;

    /** Major opcode for an invocation reply that must be combined. */
    public static final byte COMBINE = 3;

    /** Major opcode for a combined result, to be sent to all group members. */
    public static final byte COMBINE_RESULT = 4;

    /**
     * Major opcode for an invocation reply, is sent to group method invoker(s).
     */
    public static final byte INVOCATION_REPLY = 5;

    /** Major opcode for a request for a flat-combined invocation. */
    public static final byte INVOCATION_FLATCOMBINE = 6;

    /** Major opcode for a request for a binomial-combined invocation. */
    public static final byte INVOCATION_BINCOMBINE = 7;

    /** Result of an invocation with a void result. */
    public static final byte RESULT_VOID = 0;

    /** Result of an invocation with a boolean result. */
    public static final byte RESULT_BOOLEAN = 1;

    /** Result of an invocation with a byte result. */
    public static final byte RESULT_BYTE = 2;

    /** Result of an invocation with a short result. */
    public static final byte RESULT_SHORT = 3;

    /** Result of an invocation with a char result. */
    public static final byte RESULT_CHAR = 4;

    /** Result of an invocation with a int result. */
    public static final byte RESULT_INT = 5;

    /** Result of an invocation with a long result. */
    public static final byte RESULT_LONG = 6;

    /** Result of an invocation with a float result. */
    public static final byte RESULT_FLOAT = 7;

    /** Result of an invocation with a double result. */
    public static final byte RESULT_DOUBLE = 8;

    /** Result of an invocation with an Object result. */
    public static final byte RESULT_OBJECT = 9;

    /** Result of an invocation which got an exception. */
    public static final byte RESULT_EXCEPTION = 10;

    /** Registry request for group creation. */
    public static final byte CREATE_GROUP = 0;

    /** Registry request for joining a group. */
    public static final byte JOIN_GROUP = 1;

    /** Registry request for finding a group. */
    public static final byte FIND_GROUP = 2;

    /** Registry request for defining a combined invocation. */
    public static final byte DEFINE_COMBINED = 3;

    /** Registry request for a barrier. */
    public static final byte BARRIER = 4;

    /** Registry reply to a CREATE_GROUP request: success. */
    public static final byte CREATE_OK = 0;

    /** Registry reply to a CREATE_GROUP request: failure. */
    public static final byte CREATE_FAILED = 1;

    /** Registry reply to a JOIN_GROUP request: success. */
    public static final byte JOIN_OK = 10;

    /** Registry reply to a JOIN_GROUP request: unknown group.  */
    public static final byte JOIN_UNKNOWN = 11;

    /** Registry reply to a JOIN_GROUP request: group already full. */
    public static final byte JOIN_FULL = 12;

    /**
     * Registry reply to a JOIN_GROUP request: member implements wrong group
     * interface.
     */
    public static final byte JOIN_WRONG_TYPE = 13;

    /** Registry reply to a FIND_GROUP: group not known. */
    public static final byte GROUP_UNKNOWN = 20;

    /** Registry reply to a FIND_GROUP: group found. */
    public static final byte GROUP_OK = 21;

    /** Registry reply to a FIND_GROUP: group not ready yet. */
    public static final byte GROUP_NOT_READY = 22;

    /** Registry reply to a DEFINE_COMBINED request: failure. */
    public static final byte COMBINED_FAILED = 30;

    /** Registry reply to a DEFINE_COMBINED request: success. */
    public static final byte COMBINED_OK = 31;

    /** Registry reply to a BARRIER request: failure. */
    public static final byte BARRIER_FAILED = 40;

    /** Registry reply to a BARRIER request: success. */
    public static final byte BARRIER_OK = 41;
}