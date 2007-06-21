/* $Id:$ */

package ibis.ipl;

import java.util.Properties;

/**
 * Every Ibis implementation must provide an <code>IbisStarter</code>
 * which is used by the Ibis factory to check capabilities, port types,
 * and to start an Ibis instance.
 * This interface is not to be used by Ibis applications. Ibis applications
 * should use {@link IbisFactory} to create Ibis instances.
 */

public abstract class IbisStarter {

    protected IbisCapabilities capabilities;
    protected PortType[] portTypes;

    /**
     * Constructs an <code>IbisStarter</code> with the specified capabilities
     * and port types.
     * @param capabilities the required ibis capabilities.
     * @param portTypes the required port types.
     */
    public IbisStarter(IbisCapabilities capabilities, PortType[] portTypes) {
        this.capabilities = capabilities;
        this.portTypes = portTypes.clone();
    }

    /**
     * Decides if this <code>IbisStarter</code> can start an Ibis instance
     * with the desired capabilities and port types.
     * @return <code>true</code> if it can.
     */
    public abstract boolean matches();

    /**
     * Returns <code>true</code> if this starter can be used to automatically
     * start an Ibis (without the user specifying an implementation). An
     * Ibis implementation can exclude itself from the selection mechanism
     * by having this method return <code>false</code>.
     * @return <code>true</code> if this starter can be used in the selection
     * mechanism.
     */
    public abstract boolean isSelectable();

    /**
     * Returns the required capabilities that are not matched by this starter.
     * @return the unmatched ibis capabilities.
     */
    public abstract CapabilitySet unmatchedIbisCapabilities();

    /**
     * Returns the list of port types that are not matched by this starter.
     * If all required port types match, this method returns an array with
     * 0 elements.
     * @return the unmatched port types.
     */
    public abstract PortType[] unmatchedPortTypes();

    /**
     * Actually creates an Ibis instance from this starter.
     * @param handler a registry event handler.
     * @param userProperties the user properties.
     */
    public abstract Ibis startIbis(RegistryEventHandler handler,
            Properties userProperties);
}
