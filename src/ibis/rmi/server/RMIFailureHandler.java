package ibis.rmi.server;

/**
 * Non-implementation of {@link java.rmi.RMIFailureHandler}.
 * Class {@link RMISocketFactory} requires this interface, so we have to
 * provide it. However, in Ibis RMI, there is no functionality for
 * RMISocketFactories, and hence no functionality for RMIFailureHandlers.
 *
 * @author Rutger Hofman
 */
public interface RMIFailureHandler {

    public boolean failure(Exception e);

}
