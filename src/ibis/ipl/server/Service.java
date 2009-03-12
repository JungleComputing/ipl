package ibis.ipl.server;

/**
 * Interface for an Ibis Service. Any service which want to be automatically
 * started by the ibis-server, needs to implement this interface. It should also
 * have a constructor: Service(TypedProperties properties, VirtualSocketFactory
 * factory)
 */
public interface Service {

    /**
     * Returns the name of this service
     */      
    String getServiceName();
    
    /**
     * Called when the server stops.
     * 
     * @param deadline
     *            a service is allowed to block in this function for a while if
     *            it is busy. However, not after the deadline.
     */
    void end(long deadline);
    
}
