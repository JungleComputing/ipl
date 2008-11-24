/* $Id$ */

package ibis.ipl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Properties;

/**
 * Every Ibis implementation must provide an <code>IbisStarter</code>
 * which is used by the Ibis factory to check capabilities, port types,
 * and to start an Ibis instance.
 * This class is not to be used by Ibis applications. Ibis applications
 * should use {@link IbisFactory} to create Ibis instances.
 */

public abstract class IbisStarter {

    protected final IbisCapabilities capabilities;
    protected final PortType[] portTypes;
    protected final IbisStarterInfo starterInfo;

    public static class IbisStarterInfo {
        private String version;
        private Class<? extends IbisStarter> starter;
        
        public IbisStarterInfo(Class<? extends IbisStarter> starter, String version) {
            this.starter = starter;
            this.version = version;
        }
        
        public IbisStarter createInstance(
                IbisCapabilities requiredCapabilities, PortType[] portTypes)
                throws InstantiationException, IllegalAccessException,
                SecurityException, NoSuchMethodException,
                IllegalArgumentException, InvocationTargetException {
            Constructor<?> constructor = starter.getConstructor(
                    IbisCapabilities.class, PortType[].class,
                    IbisStarterInfo.class);
            return (IbisStarter) constructor.newInstance(requiredCapabilities,
                    portTypes, this);
        }
        
        public String getName() {
            return starter.getName();
        }
        
        public Class<? extends IbisStarter> getClazz() {
            return starter;
        }
        
        public String getVersion() {
            return version;
        }
    }

    /**
     * Constructs an <code>IbisStarter</code>.
     */
    public IbisStarter(IbisCapabilities capabilities, PortType[] portTypes,
            IbisStarterInfo starterInfo) {
        this.capabilities = capabilities;
        this.portTypes = portTypes.clone();
        this.starterInfo = starterInfo;
    }

    /**
     * Decides if this <code>IbisStarter</code> can start an Ibis instance
     * with the desired capabilities and port types.
     * @return
     *          <code>true</code> if it can.
     */
    public abstract boolean matches();

    /**
     * Decides if this Ibis instance is a stacking Ibis.
     * @return
     *          <code>true</code> if it is stacking.
     */
    public boolean isStacking() {
        return false;
    }

    /**
     * Returns <code>true</code> if this starter can be used to automatically
     * start an Ibis (without the user specifying an implementation). An
     * Ibis implementation can exclude itself from the selection mechanism
     * by having this method return <code>false</code>.
     * @return
     *          <code>true</code> if this starter can be used in the selection
     *          mechanism.
     */
    public abstract boolean isSelectable();

    /**
     * Returns the required capabilities that are not matched by this starter.
     * <strong>
     * Note: a stacking Ibis returns the capabilities that are required of the
     * underlying Ibis implementation.
     * </strong>
     * @return
     *          the unmatched ibis capabilities.
     */
    public abstract CapabilitySet unmatchedIbisCapabilities();

    /**
     * Returns the list of port types that are not matched by this starter.
     * If all required port types match, this method returns an array with
     * 0 elements.
     * <strong>
     * Note: a stacking Ibis returns the porttypes that are required of the
     * underlying Ibis implementation.
     * </strong>
     * @return
     *          the unmatched port types.
     */
    public abstract PortType[] unmatchedPortTypes();

    /**
     * Actually creates an Ibis instance from this starter.
     * @param handler
     *          a registry event handler.
     * @param userProperties
     *          the user properties.
     */
    public Ibis startIbis(RegistryEventHandler handler,
            Properties userProperties, String IbisImplementationVersion) {
        throw new Error("startIbis(RegistryEventHandler, Properties, String) "
                + "not implemented");
    }

    /**
     * Actually creates a stacking-ibis instance from this starter.
     * @param stack
     *          the starters for the underlying ibis implementations.
     *          The last element of the list should be a starter for a
     *          non-stacking ibis.
     * @param handler
     *          a registry event handler.
     * @param userProperties
     *          the user properties.
     */
    public Ibis startIbis(List<IbisStarter> stack,
            RegistryEventHandler handler, Properties userProperties,
            String IbisImplementationVersion) {
        if (stack.size() == 0) {
            return startIbis(handler, userProperties, IbisImplementationVersion);
        }
        throw new Error("startIbis(List<IbisStarterInfo>, RegistryEventHandler, "
                + "Properties, String) not implemented");
    }
}
