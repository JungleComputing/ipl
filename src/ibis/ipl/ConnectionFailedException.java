package ibis.ipl;

/**
 * Container class for a single failure.
 */
public class ConnectionFailedException extends java.io.IOException {

    private static final long serialVersionUID = 1L;
    private final ReceivePortIdentifier rp;
    private final IbisIdentifier id;
    private final String name;

    /**
     * Constructs a <code>ConnectionFailedException</code> for a
     * failed attempt to connect to a specific named receiveport
     * at a specific ibis instance.
     * @param detail the detail message.
     * @param identifier the Ibis identifier of the ibis instance.
     * @param name the name of the receive port.
     */
    public ConnectionFailedException(String detail, IbisIdentifier identifier, String name) {
        super(detail);
        this.id = identifier;
        this.name = name;
        this.rp = null;
    }
    
    /**
     * Constructs a <code>ConnectionFailedException</code> for a
     * failed attempt to connect to a specific named receiveport
     * at a specific ibis instance.
     * @param detail the detail message.
     * @param identifier the Ibis identifier of the ibis instance.
     * @param name the name of the receive port.
     * @param cause the cause of the failure.
     */
    public ConnectionFailedException(String detail, IbisIdentifier identifier, String name,
            Throwable cause) {
        super(detail);
        initCause(cause);
        this.id = identifier;
        this.name = name;
        this.rp = null;
    }

    /**
     * Constructs a <code>ConnectionFailedException</code> for a
     * failed attempt to connect to a specific receiveport.
     * at a specific ibis instance.
     * @param detail the detail message.
     * @param rp the receiveport identifier.
     * @param cause the cause of the failure.
     */
    public ConnectionFailedException(String detail, ReceivePortIdentifier rp,
            Throwable cause) {
        super(detail);
        initCause(cause);
        this.rp = rp;
        this.id = rp.ibis();
        this.name = rp.name();
    }
    
    /**
     * Constructs a <code>ConnectionFailedException</code> for a
     * failed attempt to connect to a specific receiveport.
     * @param detail the detail message.
     * @param rp the receiveport identifier.
     */
    public ConnectionFailedException(String detail, ReceivePortIdentifier rp) {
        super(detail);
        this.rp = rp;
        this.id = rp.ibis();
        this.name = rp.name();
    }

    /**
     * Returns the ibis identifier of the ibis instance running the
     * receive port.
     * @return the ibis identifier.
     */
    public IbisIdentifier identifier() {
        if (id == null) {
            return rp.ibis();
        }
        return id;
    }

    /**
     * Returns the receiveport identifier of the failed connection attempt.
     * If the connection attempt specified ibis identifiers and names,
     * this call may return <code>null</code>.
     * @return the receiveport identifier, or <code>null</code>.
     */
    public ReceivePortIdentifier receivePortIdentifier() {
        return rp;
    }

    /**
     * Returns the name of the receive port.
     * @return the name.
     */
    public String name() {
        if (name == null) {
            return rp.name();
        }
        return name;
    }
}