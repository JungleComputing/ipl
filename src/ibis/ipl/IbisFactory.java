/* $Id$ */

package ibis.ipl;

import ibis.util.ClassLister;
import ibis.util.TypedProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

/**
 * This is the class responsible for starting an Ibis instance.
 * During initialization, this class determines which Ibis implementations
 * are available. It does so, by finding all jar files in either the
 * class path, or all jar files in the directories indicated by the
 * ibis.impl.path property.
 * All Ibis implementations should be mentioned in the main
 * properties of the manifest of the jar file containing it, in the
 * "Ibis-Implementation" entry. This entry should contain a
 * comma- or space-separated list of class names, where each class named
 * provides an Ibis implementation. In addition, a jar-entry named
 * "capabilities" should be present in the package of this Ibis implementation,
 * and describe the specific capabilities of this Ibis implementation.
 */
public final class IbisFactory {

    private static final String PROPERTIES_FILENAME = "ibis.properties";

    /** All our own properties start with this prefix. */
    public static final String PREFIX = "ibis.";

    /** Property name of the property file. */
    public static final String FILE = PREFIX + "properties.file";

    /** Property name for the native library path. */
    public static final String LDPATH = PREFIX + "library.path";

    /** Property name for the path used to find Ibis implementations. */
    public static final String IMPLPATH = PREFIX + "impl.path";

    private static Properties defaultProperties;

    private static final String[] excludes = { "util.", "connect.", "pool.",
            "library.", "io.", "impl.", "registry.", "name", "verbose",
            "serialization", "properties." };

    static {
        Logger ibisLogger = Logger.getLogger("ibis");
        Logger rootLogger = Logger.getRootLogger();
        if (! rootLogger.getAllAppenders().hasMoreElements()
             && !ibisLogger.getAllAppenders().hasMoreElements()) {
            // No appenders defined, print to standard err by default
            PatternLayout layout = new PatternLayout("%d{HH:mm:ss} %-5p %m%n");
            WriterAppender appender = new WriterAppender(layout, System.err);
            ibisLogger.addAppender(appender);
            ibisLogger.setLevel(Level.WARN);
        }
    }

    private Class[] implList;

    private CapabilitySet[] capsList;

    private Properties properties;

    Logger logger = Logger.getLogger("ibis.ipl.IbisFactory");

    private static void load(InputStream in) {
        if (in != null) {
            try {
                defaultProperties.load(in);
            } catch(IOException e) {
                // ignored
            } finally {
                try {
                    in.close();
                } catch(Throwable e1) {
                    // ignored
                }
            }
        }
    }

    public synchronized static Properties getDefaultProperties() {
        if (defaultProperties == null) { 
            InputStream in = null;

            defaultProperties = new Properties();
            // Get the properties from the commandline. 
            Properties system = System.getProperties();

            // Then get the default properties from the classpath:
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            in = loader.getResourceAsStream(PROPERTIES_FILENAME);

            load(in);

            // Then see if there is an ibis.properties file in the users
            // home directory.
            String fn = system.getProperty("user.home") +
                system.getProperty("file.separator") + PROPERTIES_FILENAME;
            try {
                in = new FileInputStream(fn);
                load(in);
            } catch (FileNotFoundException e) {
                // ignored
            }

            // Then see if there is an ibis.properties file in the current
            // directory.
            try {
                in = new FileInputStream(PROPERTIES_FILENAME);
                load(in);
            } catch (FileNotFoundException e) {
                // ignored
            }

            // Then see if the user specified an properties file.
            String file = system.getProperty(FILE); 
            if (file != null) {
                try {
                    in = new FileInputStream(file);
                    load(in);
                } catch (FileNotFoundException e) {
                    System.err.println("User specified preferences \""
                            + file + "\" not found!");
                }
            }

            // Finally, add the properties from the command line to the result,
            // possibly overriding entries from file or the defaults.
            for (Enumeration e = system.propertyNames(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                String value = system.getProperty(key);
                defaultProperties.setProperty(key, value);
            }
        } 

        return defaultProperties;        
    }

    /** 
     * Loads a native library with ibis.
     * It might not be possible to load libraries the normal way,
     * because Ibis applications might override the bootclasspath
     * when the classlibraries have been rewritten.
     * In that case, the classloader will use the sun.boot.library.path 
     * which is not portable.
     *
     * @param name the name of the library to be loaded.
     * @param properties properties to get a library path from.
     * @exception SecurityException may be thrown by loadLibrary.
     * @exception UnsatisfiedLinkError may be thrown by loadLibrary.
     */
    public static void loadLibrary(String name, Properties properties)
            throws SecurityException, UnsatisfiedLinkError {
        String libPath = properties.getProperty(LDPATH);
        String sep = System.getProperty("file.separator");

        if (libPath != null) {
            String s = System.mapLibraryName(name);

            // System.err.println("LOADING IBIS LIB: " + libPath + sep + s);

            System.load(libPath + sep + s);
            return;
        }

        libPath = properties.getProperty("java.library.path");
        if (libPath != null) {
            System.setProperty("java.library.path", libPath);
        }
        // Fall back to regular loading.
        // This might not work, or it might not :-)
        // System.err.println("LOADING NON IBIS LIB: " + name);

        System.loadLibrary(name);
    }

    /** The currently loaded Ibises. */
    private static ArrayList<Ibis> loadedIbises = new ArrayList<Ibis>();

    private static IbisFactory defaultFactory
            = new IbisFactory(getDefaultProperties());

    /**
     * Returns a list of all Ibis implementations that are currently loaded.
     * When no Ibises are loaded, this method returns an array with no
     * elements.
     * @return the list of loaded Ibis implementations.
     */
    public static synchronized Ibis[] loadedIbises() {
        return loadedIbises.toArray(new Ibis[loadedIbises.size()]);
    }

    /**
     * Creates a new Ibis instance, based on the required capabilities,
     * and using the specified properties.
     *
     * @param requiredCapabilities capabilities required by the application.
     * @param optionalCapabilities capabilities that might come in handy, or
     * <code>null</code>.
     * @param properties properties that can be set, for instance
     * a class path for searching ibis implementations, or which registry
     * to use. There is a default, so <code>null</code> may be specified.
     * @param r a {@link ibis.ipl.ResizeHandler ResizeHandler} instance
     *  if upcalls for joining or leaving ibis instances are required,
     *  or <code>null</code>.
     * @return the new Ibis instance.
     *
     * @exception NoMatchingIbisException is thrown when no Ibis was
     *  found that matches the capabilities required.
     * @exception NextedException is thrown when no Ibis could be
     *  instantiated.
     */
    public static Ibis createIbis(CapabilitySet requiredCapabilities,
            CapabilitySet optionalCapabilities, Properties properties,
            ResizeHandler r) throws NoMatchingIbisException, NestedException {

        IbisFactory fac;

        if (properties == null) {
            fac = defaultFactory;
        } else {
            Properties tp = getDefaultProperties();
            for (Enumeration e = properties.propertyNames();
                    e.hasMoreElements();) {
                String key = (String) e.nextElement();
                String value = properties.getProperty(key);
                defaultProperties.setProperty(key, value);
            }
            fac = new IbisFactory(tp);
        }

        Ibis ibis
                = fac.createIbis(requiredCapabilities, optionalCapabilities, r);

        synchronized (IbisFactory.class) {
            loadedIbises.add(ibis);
        }

        return ibis;
    }

    /**
     * Constructs an Ibis factory, with the specified properties.
     * @param properties the specified properties.
     */
    private IbisFactory(Properties properties) {
        this.properties = properties;

        // Check capabilities
        TypedProperties tp = new TypedProperties(properties);
        tp.checkProperties(PREFIX, new String[] {}, excludes, true);

        // Check verbose
        if (tp.booleanProperty("ibis.verbose")) {
            if (logger.getEffectiveLevel().isGreaterOrEqual(Level.INFO)) {
                logger.setLevel(Level.INFO);
            }
        }

        // Obtain a list of Ibis implementations
        String implPath = properties.getProperty(IMPLPATH);
        ClassLister clstr = ClassLister.getClassLister(implPath);
        List<Class> compnts
            = clstr.getClassList("Ibis-Implementation", Ibis.class);
        implList = compnts.toArray(new Class[compnts.size()]);
        capsList = new CapabilitySet[implList.length];
        for (int i = 0; i < capsList.length; i++) {
            try {
                Class cl = implList[i];
                String packagename = cl.getPackage().getName();
                // Note: getResourceAsStream wants '/', not File.separatorChar!
                String capabilityFile = packagename.replace('.', '/')
                        + "/" + "capabilities";
                capsList[i] = CapabilitySet.load(capabilityFile);
            } catch(IOException e) {
                logger.fatal("Error while reading capabilities of "
                        + implList[i].getName(), e);
                System.exit(1);
            }
        }
    }

    private Ibis createIbis(Class<?> c, CapabilitySet caps,
            ResizeHandler resizeHandler) throws Throwable {
        Ibis impl;

        try {
            impl = (Ibis) c.getConstructor(new Class[] {ResizeHandler.class,
                    CapabilitySet.class, Properties.class}).newInstance(
                        new Object[] {resizeHandler, caps, properties});
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }

        return impl;
    }

    private Ibis createIbis(CapabilitySet requiredCapabilities,
            CapabilitySet optionalCapabilities, ResizeHandler r)
            throws NoMatchingIbisException, NestedException {

        if (logger.isInfoEnabled()) {
            logger.info("Looking for an Ibis with capabilities: "
                        + requiredCapabilities);
        }

        CapabilitySet total;

        if (optionalCapabilities != null) {
            total = requiredCapabilities.uniteWith(optionalCapabilities);
        } else {
            total = requiredCapabilities;
        }

        if (total.hasCapability(PredefinedCapabilities.WORLDMODEL_OPEN) &&
            total.hasCapability(PredefinedCapabilities.WORLDMODEL_CLOSED)) {
            throw new IbisConfigurationException("It is not allowed to ask for "
                    + "open world as well as closed world");
        }

        String ibisname = properties.getProperty("ibis.name");

        ArrayList<Class> implementations = new ArrayList<Class>();

        ArrayList<CapabilitySet> caps = new ArrayList<CapabilitySet>();

        if (ibisname == null) {
            NestedException nested = new NestedException(
                    "Could not find a matching Ibis");
            for (int i = 0; i < capsList.length; i++) {
                CapabilitySet ibissp = capsList[i];
                Class cl = implList[i];
                logger.debug("Trying " + cl.getName());
                if (requiredCapabilities.matchCapabilities(ibissp)) {
                    logger.debug("Found match!");
                    implementations.add(cl);
                    caps.add(ibissp.intersect(total));
                }
                CapabilitySet clashes
                        = requiredCapabilities.unmatchedCapabilities(ibissp);
                nested.add(cl.getName(),
                        new Exception("Unmatched capabilities: "
                            + clashes.toString()));
            }
            if (implementations.size() == 0) {
                throw new NoMatchingIbisException(nested);
            }
        } else {
            CapabilitySet ibissp = null;
            Class cl = null;
            boolean found = false;
            for (int i = 0; i < capsList.length; i++) {
                ibissp = capsList[i];
                cl = implList[i];

                String n = ibissp.getCapability("nickname");
                String classname = cl.getName();

                if (ibisname.equals(n) || ibisname.equals(classname)) {
                    found = true;
                    implementations.add(cl);
                    caps.add(ibissp.intersect(total));
                    break;
                }
            }

            if (! found) {
                throw new NoMatchingIbisException("Nickname " + ibisname
                        + " not matched");
            }

            if (!requiredCapabilities.matchCapabilities(ibissp)) {
                CapabilitySet clashes
                        = requiredCapabilities.unmatchedCapabilities(ibissp);
                logger.warn("WARNING: the " + ibisname
                       + " version of Ibis does not match the required "
                       + "capabilities.\nThe unsupported capabilities are:\n"
                       + clashes.toString()
                       + "This Ibis version was explicitly requested, "
                       + "so the run continues ...");
            }
        }

        int n = implementations.size();

        // TODO: sort implementations: the one that has the most of
        // optionalCapabilities first.

        if (logger.isInfoEnabled()) {
            String str = "";
            for (int i = 0; i < n; i++) {
                Class cl = implementations.get(i);
                str += " " + cl.getName();
            }
            logger.info("Matching Ibis implementations:" + str);
        }

        NestedException nested = new NestedException("Ibis creation failed");
        
        for (int i = 0; i < n; i++) {
            Class cl = (Class) implementations.get(i);
            logger.info("Trying " + cl.getName());
            while (true) {
                try {
                    return createIbis(cl, caps.get(i), r);
                } catch (ConnectionRefusedException e) {
                    // retry
                } catch (Throwable e) {
                    nested.add(cl.getName(), e);
                    if (i == n - 1) {
                        // No more Ibis to try.
                        throw nested;
                    }
                    if (logger.isInfoEnabled()) {
                        logger.info("Could not instantiate " + cl.getName(), e);
                    }
                    break;
                }
            }
        }
        throw nested;
    }
}
