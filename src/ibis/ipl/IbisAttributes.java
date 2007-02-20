package ibis.ipl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public final class IbisAttributes {
    private static final String DEFAULT_FILE = "ibis.attributes";

    static final String PREFIX = "ibis.";

    public static final String FILE = PREFIX + "file";

    public static final String LDPATH = PREFIX + "library.path";

    public static final String REGISTRYIMPL = PREFIX + "registry.impl";

    public static final String IMPLPATH = PREFIX + "impl.path";

    private static final String[] defaults = new String[] {
        REGISTRYIMPL,
        "ibis.impl.registry.tcp.NameServerClient"
    };

    private static TypedProperties defaultAttributes;

    private static TypedProperties getAttributeFile(String file) {

        InputStream in = null;

        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            // ignored
        }

        if (in == null) {
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            in = loader.getResourceAsStream(file);

            if (in == null) {
                return null;
            }
        }

        try {
            TypedProperties p = new TypedProperties();
            p.load(in);
            return p;
        } catch (IOException e) {
            try {
                in.close();
            } catch (Exception x) {
                // ignore
            }
        }
        return null;
    }

    public static TypedProperties getDefaultAttributes() {

        if (defaultAttributes == null) { 
            defaultAttributes = new TypedProperties();

            // Start by inserting the default values.            
            for (int i=0;i<defaults.length;i+=2) { 
                defaultAttributes.put(defaults[i], defaults[i+1]);            
            }

            // Get the properties from the commandline. 
            // Run through filter to get all properties at the toplevel,
            // not in some nested (default) level, so that the putAll below
            // does what we want.
            TypedProperties system = 
                new TypedProperties(System.getProperties()).filter(null);

            // Check what property file we should load.
            String file = system.getProperty(FILE, DEFAULT_FILE); 

            // If the file is not explicitly set to null, we try to load it.
            if (file != null) {

                TypedProperties fromFile = getAttributeFile(file);

                if (fromFile == null) { 
                    if (!file.equals(DEFAULT_FILE)) { 
                        // If we fail to load the user specified file, we give
                        // an error, since only the default file may fail
                        // silently.                     
                        System.err.println("User specified preferences \""
                                + file + "\" not found!");
                    }                                            
                } else {                  
                    // If we managed to load the file, we add the properties to
                    // the 'defaultAttributes' possibly overwriting defaults.
                    defaultAttributes.putAll(fromFile);
                }
            }

            // Finally, add the properties from the command line to the result,
            // possibly overriding entries from file or the defaults.            
            defaultAttributes.putAll(system);
        } 

        return defaultAttributes;        
    }
}
