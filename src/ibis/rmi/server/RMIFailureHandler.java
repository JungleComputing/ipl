package ibis.rmi.server;

/**
 * Class {@link RMISocketFactory} requires this interface, so we have to
 * provide it. However, in Ibis RMI, there is no functionality for
 * RMISocketFactories, and hence no functionality for RMIFailureHandlers.
 *
 * @author Rutger Hofman
 */
public interface RMIFailureHandler {

    /**
     * No failure handler in Ibis RMI, but the socket factory requires
     * this.
     */
    public boolean failure(Exception e);

}