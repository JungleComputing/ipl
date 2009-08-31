package ibis.ipl.server;

/**
 * Interface for an Ibis Service. Any service that wants to be automatically
 * started by the ibis-server must implement this interface. It must also
 * have a constructor with the signature: Service(TypedProperties properties,
 * VirtualSocketFactory factory).
 */
public interface Service {

    /**
     * Returns the name of this service.
     */
    String getServiceName();

    /**
     * Called when the server stops.
     * 
     * @param deadline
     *            a service is allowed to block in this function for a while if
     *            it is busy. However, it may not block beyond the deadline.
     */
    void end(long deadline);
}
