
package ibis.ipl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**

 */
public final class IbisCapabilities extends CapabilitySet {

    /** Prefix for worldmodel capabilities. */
    public final static String WORLDMODEL = "worldmodel";

    /** Prefix for registry capabilities. */
    final static String REGISTRY = "registry";

    /**
     * Boolean capability, set when the Ibises that can join the run are
     * determined at the start of the run. This enables the methods
     * {@link Ibis#totalNrOfIbisesInPool()} and {@link Ibis#waitForAll()}.
     */
    public final static String WORLDMODEL_CLOSED = WORLDMODEL + ".closed";
    
    /**
     * Boolean capability, does not really mean anything, except that it is
     * the complement of WORLDMODEL_CLOSED.
     */
    public final static String WORLDMODEL_OPEN = WORLDMODEL + ".open";

 
    /** Boolean capability, indicating that registry event downcalls are supported. */
    public final static String REGISTRY_DOWNCALLS = REGISTRY + ".downcalls";

    /**
     * Boolean capability, indicating that registry event upcall handlers are
     * supported.
     */
    public final static String REGISTRY_UPCALLS = REGISTRY + ".upcalls";
    
    /** 
     * Constructor for an IbisCapabilities object.
     * @param capabilities the capabilities.
     */
    public IbisCapabilities(String... capabilities) {
        super(capabilities);
    }
    
    /**
     * Constructs a port type from the specified properties.
     * @param sp the properties.
     */
    protected IbisCapabilities(Properties sp) {
        super(sp);
    }
    
    public IbisCapabilities(CapabilitySet s) {
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
