
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

    /** Prefix for worldmodel capabilities. */
    public final static String WORLDMODEL = "worldmodel";

    /** Prefix for registry capabilities. */
    final static String REGISTRY = "registry";

    /**
     * Boolean capability, set when the Ibises that can join the run are
     * determined at the start of the run. This enables the methods
     * {@link Ibis#getPoolSize()} and {@link Ibis#waitForAll()}.
     */
    public final static String WORLDMODEL_CLOSED = WORLDMODEL + ".closed";
    
    /**
     * Boolean capability, does not really mean anything, except that it is
     * the complement of WORLDMODEL_CLOSED.
     */
    public final static String WORLDMODEL_OPEN = WORLDMODEL + ".open";

    /** Boolean capability, indicating that registry event downcalls are supported. */
    public final static String REGISTRY_DOWNCALLS = REGISTRY + ".downcalls";

    /** Boolean capability, indicating that registry event handlers are supported. */
    public final static String REGISTRY_UPCALLS = REGISTRY + ".upcalls";
    
    /** 
     * Constructor for an IbisCapabilities object.
     * @param capabilities the capabilities.
     */
    public IbisCapabilities(String... capabilities) {
        super(capabilities);
    }
    
    /**
     * Constructs an IbisCapabilities object from the specified properties.
     * @param sp the properties.
     */
    protected IbisCapabilities(Properties sp) {
        super(sp);
    }

    /**
     * Constructs an IbisCapabilities from the specified capabilityset.
     * @param s the capabilityset.
     */
    protected IbisCapabilities(CapabilitySet s) {
         super(s);
    }
    
    /**
     * Reads and returns the capabilities from the specified file name, which is
     * searched for in the classpath.
     * @param name the file name.
     * @exception IOException is thrown when an IO error occurs.
     */
    public static IbisCapabilities load(String name) throws IOException {
        InputStream in
            = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
        if (in == null) {
            throw new IOException("Could not open " + name);
        }
        return load(in);
    }

    /**
     * Reads and returns the capabilities from the specified input stream.
     * @param in the input stream.
     * @exception IOException is thrown when an IO error occurs.
     */
    public static IbisCapabilities load(InputStream in) throws IOException {
        Properties p = new Properties();
        p.load(in);
        in.close();
        return new IbisCapabilities(p);
    }
}
