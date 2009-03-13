/* $Id$ */

package ibis.ipl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This is the class responsible for starting an Ibis instance. During
 * initialization, this class determines which Ibis implementations are
 * available. It does so by finding all jar files in either the class path or
 * all jar files in the directories indicated by the ibis.ipl.impl.path
 * property. All Ibis implementations should be mentioned in the main properties
 * of the manifest of the jar file containing it, in the "Ibis-Starter" entry.
 * This entry should contain a comma- or space-separated list of class names,
 * where each class named provides an {@link IbisStarter} implementation. In
 * addition, a property "Ibis-IPL-Version" should be defined in the manifest,
 * containing a version number (e.g. 2.1).
 */
public final class IbisFactory {

    // START OF ImplementationInfo INNER CLASS

    /**
     * (Inner Class) Info for an implementation of Ibis. These are created
     * either from the manifest of a jar file, or the IPL_MANIFEST property file
     * 
     * @author Niels Drost
     * 
     */
    public static class ImplementationInfo {

        /**
         * Class used to start this implementation
         */
        private final Class<? extends IbisStarter> starter;

        /**
         * Short name of this implementation. Usually the network stack it is
         * based on, e.g. "tcp" or "mpi"
         */
        private final String nickName;

        /**
         * Which version of the IPL does this implementation implement.
         */
        private final String iplVersion;

        /**
         * Version identifier of this implementation. Usually a checksum created
         * from the class files of the implementation.
         */
        private final String implementationVersion;

        public ImplementationInfo(Class<? extends IbisStarter> starter,
                String nickName, String iplVersion, String implementationVersion) {
            this.starter = starter;
            this.nickName = nickName;
            this.iplVersion = iplVersion;
            this.implementationVersion = implementationVersion;
        }

        public IbisStarter createInstance(
                IbisCapabilities requiredCapabilities, PortType[] portTypes)
                throws InstantiationException, IllegalAccessException,
                SecurityException, NoSuchMethodException,
                IllegalArgumentException, InvocationTargetException {
            Constructor<?> constructor = starter.getConstructor(
                    IbisCapabilities.class, PortType[].class,
                    ImplementationInfo.class);
            return (IbisStarter) constructor.newInstance(requiredCapabilities,
                    portTypes, this);
        }

        public Class<? extends IbisStarter> getStarterClass() {
            return starter;
        }

        public String getVersion() {
            return iplVersion;
        }

        /**
         * Class used to start this implementation.
         * 
         * @return the starter class
         */
        public Class<? extends IbisStarter> getStarter() {
            return starter;
        }

        /**
         * Short name of this implementation. Usually the network stack it is
         * based on, e.g. "tcp" or "mpi"
         * 
         * @return the nickName
         */
        public String getNickName() {
            return nickName;
        }

        /**
         * Which version of the IPL does this implementation implement.
         * 
         * @return the IPL version
         */
        public String getIplVersion() {
            return iplVersion;
        }

        /**
         * Version identifier of this implementation. Usually a checksum created
         * from the class files of the implementation.
         * 
         * @return the implementation version
         */
        public String getImplementationVersion() {
            return implementationVersion;
        }

        public String toString() {
            return nickName;
        }
    }

    // END OF ImplementationInfo INNER CLASS

    private static final String IPL_VERSION_STRING = "Ibis-IPL-Version";

    private static final String STARTER_CLASS_STRING = "Ibis-Starter-Class";

    private static final String IMPLEMENTATION_VERSION_STRING = "Ibis-Implementation-Version";

    private static final String NICKNAME_STRING = "Ibis-NickName";

    public static final String IPL_MANIFEST_FILE = "ibis/ipl/IPL_MANIFEST";

    // Map of factories. One for each implementation path
    private static final Map<String, IbisFactory> factories = new HashMap<String, IbisFactory>();

    private static IbisFactory defaultFactory;

    static final String VERSION = "2.2";

    private static synchronized IbisFactory getFactory(String implPath,
            Properties properties) {
        if (implPath == null) {
            if (defaultFactory == null) {
                defaultFactory = new IbisFactory(null, properties);
            }

            return defaultFactory;
        } else {
            IbisFactory factory = factories.get(implPath);

            if (factory == null) {
                factory = new IbisFactory(implPath, properties);
                factories.put(implPath, factory);
            }

            return factory;
        }
    }

    /**
     * List of all available implementations
     */
    private Map<String, ImplementationInfo> implementations;

    /**
     * Constructs an Ibis factory, with the specified search path.
     * 
     * @param implPath
     *                the path to search for implementations.
     */
    private IbisFactory(String implementationPath, Properties properties) {

        implementations = new HashMap<String, ImplementationInfo>();

        // load implementations from jar path

        loadImplementationsFromJars(implementationPath);

        // load implementations from manifest property file

        loadImplementationsFromManifestFile();

        if (implementations.size() == 0) {
            throw new IbisConfigurationException(
                    "Cannot find any Ibis implementations");
        }
    }

    /**
     * Creates a new Ibis instance, based on the required capabilities and port
     * types. As the set of properties, the default properties are used.
     * 
     * @param requiredCapabilities
     *                ibis capabilities required by the application.
     * @param registryEventHandler
     *                a
     *                {@link ibis.ipl.RegistryEventHandler RegistryEventHandler}
     *                instance, or <code>null</code>.
     * @param portTypes
     *                the list of port types required by the application.
     * @return the new Ibis instance.
     * 
     * @exception IbisCreationFailedException
     *                    is thrown when no Ibis was found that matches the
     *                    capabilities required, or a matching Ibis could not be
     *                    instantiated for some reason.
     */
    public static Ibis createIbis(IbisCapabilities requiredCapabilities,
            RegistryEventHandler registryEventHandler, PortType... portTypes)
            throws IbisCreationFailedException {
        return createIbis(requiredCapabilities, null, true,
                registryEventHandler, null, portTypes);
    }

    /**
     * Creates a new Ibis instance, based on the required capabilities and port
     * types, and using the specified properties.
     * 
     * @param requiredCapabilities
     *                ibis capabilities required by the application.
     * @param properties
     *                properties that can be set, for instance a class path for
     *                searching ibis implementations, or which registry to use.
     *                There is a default, so <code>null</code> may be
     *                specified.
     * @param addDefaultConfigProperties
     *                adds the default properties, loaded from the system
     *                properties, a "ibis.properties" file, etc, for as far as
     *                these are not set in the <code>properties</code>
     *                parameter.
     * @param registryEventHandler
     *                a
     *                {@link ibis.ipl.RegistryEventHandler RegistryEventHandler}
     *                instance, or <code>null</code>.
     * @param authenticationObject
     *                an object which can be used by the registry to
     *                authenticate this ibis
     * @param portTypes
     *                the list of port types required by the application. Can be
     *                an empty list, but not null.
     * @return the new Ibis instance.
     * 
     * @exception IbisCreationFailedException
     *                    is thrown when no Ibis was found that matches the
     *                    capabilities required, or a matching Ibis could not be
     *                    instantiated for some reason.
     */
    @SuppressWarnings("unchecked")
    public static Ibis createIbis(IbisCapabilities requiredCapabilities,
            Properties properties, boolean addDefaultConfigProperties,
            RegistryEventHandler registryEventHandler,
            Object authenticationObject, PortType... portTypes)
            throws IbisCreationFailedException {

        Properties combinedProperties = new Properties();

        // add default properties, if required
        if (addDefaultConfigProperties) {
            Properties defaults = IbisProperties.getDefaultProperties();

            for (Enumeration<String> e = (Enumeration<String>) defaults
                    .propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement();
                String value = defaults.getProperty(key);
                combinedProperties.setProperty(key, value);
            }
        }

        // add user properties
        if (properties != null) {
            for (Enumeration<String> e = (Enumeration<String>) properties
                    .propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement();
                String value = properties.getProperty(key);
                combinedProperties.setProperty(key, value);
            }
        }

        String implPath = combinedProperties
                .getProperty(IbisProperties.IMPLEMENTATION_PATH);
        IbisFactory factory = getFactory(implPath, combinedProperties);

        return factory.createIbis(registryEventHandler, requiredCapabilities,
                combinedProperties, portTypes, authenticationObject);
    }

    private List<IbisStarter> findIbisStack(IbisCapabilities capabilities,
            PortType[] portTypes, List<IbisStarter> selected, String ibisName,
            IbisCreationFailedException creationException, boolean verbose) {

        IbisCapabilities caps = capabilities;
        PortType[] types = portTypes;
        boolean faulty = false;

        ArrayList<IbisStarter> starters = new ArrayList<IbisStarter>();

        for (IbisFactory.ImplementationInfo info : implementations.values()) {
            if (verbose) {
                System.err.println("Instantiating " + info);
            }

            // Try to instantiate the starter.
            try {
                starters.add(info.createInstance(caps, types));
            } catch (Throwable e) {
                faulty = true;
                // Oops, could not instantiate starter.
                if (creationException != null) {
                    creationException.add(info.getNickName(), e);
                }
                if (verbose) {
                    System.err.println("Could not instantiate "
                            + info.getNickName() + ": " + e);
                }
                continue;
            }
        }
        if (faulty && creationException != null) {
            return null;
        }

        // First try non-stacking Ibis implementations.
        for (IbisStarter instance : starters) {
            // If it is selectable, or an Ibis name was specified,
            // try it.
            if ((instance.isSelectable() || ibisName != null)
                    && !instance.isStacking()) {
                if (verbose) {
                    System.err.println("Matching with "
                            + instance.implementationInfo.getNickName());
                }
                if (instance.matches()) {
                    selected.add(instance);
                    if (verbose) {
                        System.err.println("Class "
                                + instance.implementationInfo.getNickName()
                                + " selected");
                    }
                    return selected;
                }
                // Find out why it did not match.
                String unmatchedCapabilities = instance
                        .unmatchedIbisCapabilities().toString();
                PortType[] unmatchedTypes = instance.unmatchedPortTypes();
                StringBuffer str = new StringBuffer();
                str.append("Unmatched IbisCapabilities: ");
                str.append(unmatchedCapabilities);
                if (unmatchedTypes.length > 0) {
                    str.append("\nUnmatched PortTypes: ");
                    for (PortType tp : unmatchedTypes) {
                        str.append("    ");
                        str.append(tp.toString());
                        str.append("\n");
                    }
                } else {
                    str.append("\n");
                }
                if (creationException != null) {
                    creationException.add(instance.getClass().getName(),
                            new IbisConfigurationException(str.toString()));
                }

                if (verbose) {
                    System.err.println("Class " + instance.getClass().getName()
                            + " does not match:\n" + str.toString());
                }
            } else {
                if (verbose) {
                    System.err.println("Class " + instance.getClass().getName()
                            + " is stacking or not selectable.");
                }
            }
        }

        // Now try stacking Ibis implementations.
        for (IbisStarter instance : starters) {
            if ((instance.isSelectable() || ibisName != null)
                    && instance.isStacking() && !selected.contains(instance)
                    && instance.matches()) {
                if (verbose) {
                    System.err.println("Class " + instance.getClass().getName()
                            + " selected");
                }
                List<IbisStarter> newList = new ArrayList<IbisStarter>(selected);
                newList.add(instance);
                newList = findIbisStack(new IbisCapabilities(instance
                        .unmatchedIbisCapabilities()), instance
                        .unmatchedPortTypes(), newList, null, null, verbose);
                if (newList != null) {
                    return newList;
                }
                if (creationException != null) {
                    creationException
                            .add(
                                    instance.getClass().toString(),
                                    new IbisConfigurationException(
                                            "Could not create valid stack with this ibis on top"));
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Ibis createIbis(RegistryEventHandler registryEventHandler,
            IbisCapabilities requiredCapabilities, Properties properties,
            PortType[] portTypes, Object authenticationObject)
            throws IbisCreationFailedException {
        if (requiredCapabilities == null) {
            throw new IbisConfigurationException("capabilities not specified");
        }

        if (portTypes == null) {
            throw new IbisConfigurationException("port types not specified");
        }

        String verboseValue = properties.getProperty(IbisProperties.VERBOSE);
        // see if the user specified "verbose"
        boolean verbose = verboseValue != null
                && (verboseValue.equals("1") || verboseValue.equals("on")
                        || verboseValue.equals("")
                        || verboseValue.equals("true") || verboseValue
                        .equals("yes"));

        if (verbose) {
            System.err.println("Looking for an Ibis with capabilities: "
                    + requiredCapabilities);
            System.err.println("(ibis) Properties:");
            for (Enumeration e = properties.propertyNames(); e
                    .hasMoreElements();) {
                String key = (String) e.nextElement();
                if (key.startsWith("ibis")) {
                    String value = properties.getProperty(key);
                    System.err.println(key + " = " + value);
                }
            }
        }

        String ibisName = properties.getProperty(IbisProperties.IMPLEMENTATION);

        if (ibisName != null) {
            String[] capabilities = requiredCapabilities.getCapabilities();
            String[] newCapabilities = new String[capabilities.length + 1];
            for (int i = 0; i < capabilities.length; i++) {
                newCapabilities[i] = capabilities[i];
            }
            newCapabilities[capabilities.length] = "nickname." + ibisName;
            capabilities = newCapabilities;
            requiredCapabilities = new IbisCapabilities(newCapabilities);
        }

        if (verbose) {
            StringBuffer str = new StringBuffer();
            str.append("Ibis implementations:");
            for (ImplementationInfo info : implementations.values()) {
                str.append(" ");
                str.append(info.getNickName());
            }
            System.err.println(str.toString());
        }

        IbisCreationFailedException creationException = new IbisCreationFailedException(
                "Ibis creation failed");

        //
        // Factory does some initial sanity checks.
        // Port types can only specify a single connection capability,
        // and must specify a serialization.

        boolean faulty = false;
        for (PortType tp : portTypes) {
            // Check sanity of port types.
            int cnt = 0;
            if (tp.hasCapability(PortType.CONNECTION_MANY_TO_MANY)) {
                cnt++;
            }
            if (tp.hasCapability(PortType.CONNECTION_ONE_TO_ONE)) {
                cnt++;
            }
            if (tp.hasCapability(PortType.CONNECTION_ONE_TO_MANY)) {
                cnt++;
            }
            if (tp.hasCapability(PortType.CONNECTION_MANY_TO_ONE)) {
                cnt++;
            }
            if (cnt != 1) {
                creationException
                        .add(
                                "Ibis factory",
                                new IbisConfigurationException(
                                        "PortType "
                                                + tp
                                                + " should specify exactly one connection type"));
                faulty = true;
            }
            String[] caps = tp.getCapabilities();
            boolean ok = false;
            for (String s : caps) {
                if (s.startsWith(PortType.SERIALIZATION)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                creationException.add("Ibis factory",
                        new IbisConfigurationException("Port type " + tp
                                + " should specify serialization"));
                faulty = true;
            }
        }

        // If a registryEventHandler is specified, the membership capability
        // must be requested as well.

        if (registryEventHandler != null
                && !requiredCapabilities
                        .hasCapability(IbisCapabilities.MEMBERSHIP_UNRELIABLE)
                && !requiredCapabilities
                        .hasCapability(IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED)) {
            creationException.add("Ibis factory",
                    new IbisConfigurationException(
                            "RegistryEventHandler specified but no "
                                    + " membership capability requested"));
            faulty = true;
        }

        if (faulty) {
            // There is some error in the user-specified capabilities or
            // port types.
            throw creationException;
        }

        List<IbisStarter> stack = findIbisStack(requiredCapabilities,
                portTypes, new ArrayList<IbisStarter>(), ibisName,
                creationException, verbose);

        if (stack == null) {
            creationException.add("Ibis factory",
                    new IbisConfigurationException("No matching Ibis found"));
            throw creationException;
        }

        IbisStarter starter = stack.remove(0);

        try {
            return starter.startIbis(stack, registryEventHandler, properties, authenticationObject);
        } catch (Throwable e) {
            creationException.add("" + starter.implementationInfo.getNickName()
                    + " gave exception ", e);
            throw creationException;
        }
    }

    /**
     * Create a JarFile from a given File.
     */
    private static JarFile getJarFile(File file) {
        try {
            if (!file.isFile() || !file.getName().endsWith(".jar")) {
                // not a jar file
                return null;
            }
            JarFile result = new JarFile(file, true);
            Manifest manifest = result.getManifest();
            if (manifest != null) {
                manifest.getMainAttributes();
                return result;
            }
        } catch (IOException e) {
            System.err.println("Could not create jar file from file: " + file);
        }
        return null;
    }

    /**
     * This method reads all jar files found in the specified path, and stores
     * them in a list.
     */
    private static JarFile[] readJarFiles(String path) {
        ArrayList<JarFile> result = new ArrayList<JarFile>();

        StringTokenizer st = new StringTokenizer(path, File.pathSeparator);

        while (st.hasMoreTokens()) {
            String dir = st.nextToken();

            File file = new File(dir);

            if (file.isFile()) {
                JarFile jarFile = getJarFile(file);
                if (jarFile != null) {
                    result.add(jarFile);
                }
            } else if (file.isDirectory()) {
                File[] children = file.listFiles();
                for (File child : children) {
                    JarFile jarFile = getJarFile(child);
                    if (jarFile != null) {
                        result.add(jarFile);
                    }
                }
            } else {
                System.err.println("Not a file/directory: " + file);
            }
        }
        return result.toArray(new JarFile[0]);
    }

    @SuppressWarnings("unchecked")
    private static ImplementationInfo loadInfoFromJar(JarFile jar,
            ClassLoader classLoader) {

        System.err.println("Loading implementation from " + jar.getName());

        try {
            Manifest manifest = jar.getManifest();

            Attributes attributes = manifest.getMainAttributes();

            String iplVersion = attributes.getValue(IPL_VERSION_STRING);
            String implementationVersion = attributes
                    .getValue(IMPLEMENTATION_VERSION_STRING);
            String nickName = attributes.getValue(NICKNAME_STRING);
            String starterClass = attributes.getValue(STARTER_CLASS_STRING);

            if (iplVersion == null || !iplVersion.startsWith(VERSION)
                    || implementationVersion == null || nickName == null
                    || starterClass == null) {
                return null;
            }

            return new ImplementationInfo((Class<? extends IbisStarter>) Class
                    .forName(starterClass, false, classLoader), nickName,
                    iplVersion, implementationVersion);
        } catch (Exception e) {
            System.err.println("Could not load ibis from jar: " + jar.getName()
                    + ": " + e);
            return null;
        }
    }

    private void loadImplementationsFromJars(String implementationPath) {
        JarFile[] jarFiles;

        if (implementationPath == null) {
            implementationPath = System.getProperty("java.class.path");
        }
        jarFiles = readJarFiles(implementationPath);

        // create ClassLoader for jar files

        URL[] urls = new URL[jarFiles.length];

        for (int i = 0; i < jarFiles.length; i++) {
            try {
                File f = new File(jarFiles[i].getName());
                urls[i] = f.toURI().toURL();
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        ClassLoader classLoader = new URLClassLoader(urls, this.getClass()
                .getClassLoader());

        for (int i = 0; i < jarFiles.length; i++) {
            ImplementationInfo info = loadInfoFromJar(jarFiles[i], classLoader);

            if (info != null) {
                implementations.put(info.getNickName(), info);
            }
        }
    }

    private void loadImplementationsFromManifestFile() {

    }

}
