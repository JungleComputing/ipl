package ibis.rmi;

/**
 * The <code>Protocol</code> interface defines some constants for the
 * Ibis RMI wire communication protocol.
 */
public interface Protocol {
    /** Sent when a remote invocation resulted in an exception. */
    public final byte EXCEPTION    = 0;

    /** Sent when a remote invocation did not result in an exception. */
    public final byte RESULT       = 1;
}
