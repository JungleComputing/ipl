package ibis.gmi;

/**
 * The {@link GroupMessage} class is a container class for keeping replies
 * around until they can be processed.
 */
public final class GroupMessage { 
    /** So that they can be kept in a linked list. */
    protected GroupMessage next;

    /** The rank number of the group member responsible for this reply. */
    public int rank;

    /** The reply, in case it is a boolean. */
    public boolean booleanResult;

    /** The reply, in case it is a byte. */
    public byte byteResult;

    /** The reply, in case it is a short. */
    public short shortResult;

    /** The reply, in case it is a char. */
    public char charResult;

    /** The reply, in case it is an int. */
    public int intResult;

    /** The reply, in case it is a long. */
    public long longResult;

    /** The reply, in case it is a float. */
    public float floatResult;

    /** The reply, in case it is a double. */
    public double doubleResult;

    /** The reply, in case it is an Object. */
    public Object objectResult;

    /** The reply, in case the invocation resulted in an exception. */
    public Exception exceptionResult;
} 
