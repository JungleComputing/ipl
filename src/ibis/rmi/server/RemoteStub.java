package ibis.rmi.server;

/**
 * <code>RemoteStub</code> is the common superclass for client stubs.
 */
abstract public class RemoteStub extends RemoteObject {
    /**
     * Constructs a <code>RemoteStub</code>.
     */
    protected RemoteStub() {
        super();
    }

    /**
     * Constructs a <code>RemoteStub</code> with the specified remote
     * reference.
     * @param ref the remote reference
     */
    protected RemoteStub(RemoteRef ref) {
        super(ref);
    }
}