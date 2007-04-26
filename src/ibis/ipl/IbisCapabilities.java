
package ibis.ipl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class describes the capabilities of an ibis instance.
 * Combined with a list of {@link PortType} it is
 * used to select a particular Ibis implementation.
 * See the
 * {@link IbisFactory#createIbis(IbisCapabilities, Properties, RegistryEventHandler, PortType...) createIbis}
 * method from {@link IbisFactory}.       
 */
public final class IbisCapabilities extends CapabilitySet {

    /** Prefix for registry capabilities. */
    final static String REGISTRY = "registry";

    /** Prefix for worldmodel capabilities. */
    public final static String WORLDMODEL = REGISTRY + ".worldmodel";
    
    /**
     * Capability, set when the Ibises that can join the pool are
     * determined at the start of the run. This enables the methods
     * {@link Ibis#getPoolSize()} and {@link Ibis#waitForAll()}.
     */
    public final static String CLOSEDWORLD = WORLDMODEL + ".closed";
    
    /** Capability, indicating that signals are supported. */
    public final static String SIGNALS = REGISTRY + ".signals";

    /** Capability, indicating that elections are supported. */
    public final static String ELECTIONS = REGISTRY + ".elections";
    
    /** Capability indicating that membership administration is supported. */
    public final static String MEMBERSHIP = REGISTRY + ".membershipManagement";
    
    /** Capability indicating that joins/leaves are totally ordered. */
    public final static String MEMBERSHIP_ORDERED
            = MEMBERSHIP + ".ordered";
    
    /** Capability indicating that joins/leaves are reliable. */
    public final static String MEMBERSHIP_RELIABLE
            = MEMBERSHIP + ".reliable";
    
    /** Capability indicating an Ibis that can deal with malleability. */
    public final static String MALLEABLE = "ibis.malleable";
    
    /** 
     * Constructor for an IbisCapabilities object.
     * @param capabilities the capabilities.
     */
    public IbisCapabilities(String... capabilities) {
        super(capabilities);
    }
    
    /**
     * Constructs an IbisCapabilities object from the specified properties.
     * @param properties the properties.
     */
    protected IbisCapabilities(Properties properties) {
        super(properties);
    }

    /**
     * Constructs an IbisCapabilities from the specified capabilityset.
     * @param capabilitySet the capabilityset.
     */
    protected IbisCapabilities(CapabilitySet capabilitySet) {
         super(capabilitySet);
    }
    
    /**
     * Reads and returns the capabilities from the specified file name, which is
     * searched for in the classpath.
     * @param capabilityFileName the file name.
     * @exception IOException is thrown when an IO error occurs.
     */
    public static IbisCapabilities load(String capabilityFileName) throws IOException {
        InputStream input
            = ClassLoader.getSystemClassLoader().getResourceAsStream(capabilityFileName);
        if (input == null) {
            throw new IOException("Could not open " + capabilityFileName);
        }
        return load(input);
    }

    /**
     * Reads and returns the capabilities from the specified input stream.
     * @param input the input stream.
     * @exception IOException is thrown when an IO error occurs.
     */
    public static IbisCapabilities load(InputStream input) throws IOException {
        Properties properties = new Properties();
        properties.load(input);
        input.close();
        return new IbisCapabilities(properties);
    }
}
