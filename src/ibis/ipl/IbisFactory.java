/* $Id$ */

package ibis.ipl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
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

    private static final String IPL_VERSION_STRING = "Ibis-IPL-Version";

    private static final String STARTER_CLASS_STRING = "Ibis-Starter-Class";

    private static final String IMPLEMENTATION_VERSION_STRING = "Ibis-Implementation-Version";

    private static final String NICKNAME_STRING = "Ibis-NickName";

    /**
     * Name of a backup manifest file, which is used for systems on which
     * a manifest file cannot be loaded (i.e. Android),
     */
    public static final String IPL_MANIFEST_FILE = "ibis/ipl/IPL_MANIFEST";

    /**
     * Nickname of the default Ibis implementation. This is the Ibis implementation
     * used, unless the user explicitly requests another implementation.
     */
    public static final String DEFAULT_IMPLEMENTATION = "smartsockets";

    /** Map of factories, one for each implementation path. */
    private static final Map<String, IbisFactory> factories = new HashMap<String, IbisFactory>();

    private static IbisFactory defaultFactory;

    private static final String VERSION = "2.2";
    
    private static Properties manifestProperties = new Properties();
    
    static {
        ClassLoader classLoader = IbisFactory.class.getClassLoader();
        InputStream inputStream = classLoader
                .getResourceAsStream(IPL_MANIFEST_FILE);

        if (inputStream == null) {
            System.err.println(
                    "Warning: could not load properties from manifest property file");
        } else {
            try {
                manifestProperties.load(inputStream);
            } catch (IOException e) {
                System.err.println(
                        "Warning: could not load properties from manifest property file");
            }
        }
    }
    
    public static String getManifestProperty(String p) {
        return manifestProperties.getProperty(p, null);
    }

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
     * List of all available implementations.
     */
    private Map<String, IbisStarter> implementations;

    private IbisFactory() {
        // DO NOT USE
    }

    /**
     * Constructs an Ibis factory, with the specified search path.
     * 
     * @param implementationPath
     *            the path to search for implementations.
     * @param properties
     *            the properties to be used.
     */
    private IbisFactory(String implementationPath, Properties properties) {

        implementations = new HashMap<String, IbisStarter>();

        // load implementations from jar path

        loadIbisesFromJars(implementationPath);

        // load implementations from manifest property file

        loadIbisesFromManifestFile();

        if (implementations.size() == 0) {
            throw new IbisConfigurationException(
                    "Cannot find any Ibis implementations");
        }
    }

    private static boolean isVerbose(Properties properties) {
        // see if the user specified "verbose"
        String verboseValue = properties.getProperty(IbisProperties.VERBOSE);
        return verboseValue != null
                && (verboseValue.equals("1") || verboseValue.equals("on")
                        || verboseValue.equals("")
                        || verboseValue.equals("true") || verboseValue
                        .equals("yes"));
    }

    /**
     * Creates a new Ibis instance, making sure that the Ibis implementation
     * chosen implements the specified capabilities and port types.
     * 
     * @param requiredCapabilities
     *            ibis capabilities required by the application.
     * @param registryEventHandler
     *            a {@link ibis.ipl.RegistryEventHandler RegistryEventHandler}
     *            instance, or <code>null</code>.
     * @param portTypes
     *            the list of port types required by the application.
     * @return the new Ibis instance.
     * 
     * @exception IbisCreationFailedException
     *                is thrown when the chosen Ibis implementation cannot be
     *                created for some reason.
     */
    public static Ibis createIbis(IbisCapabilities requiredCapabilities,
            RegistryEventHandler registryEventHandler, PortType... portTypes)
            throws IbisCreationFailedException {
        return createIbis(requiredCapabilities, null, true,
                registryEventHandler, portTypes);
    }

    /**
     * Creates a new Ibis instance, based on the specified properties,
     * making sure that the Ibis implementation
     * chosen implements the specified capabilities and port types.
     * 
     * @param requiredCapabilities
     *            ibis capabilities required by the application.
     * @param properties
     *            properties that can be set, for instance a class path for
     *            searching ibis implementations, or which registry to use.
     *            There is a default, so <code>null</code> may be specified.
     * @param addDefaultConfigProperties
     *            adds the default properties, loaded from the system
     *            properties, a "ibis.properties" file, etc, for as far as these
     *            are not set in the <code>properties</code> parameter.
     * @param registryEventHandler
     *            a {@link ibis.ipl.RegistryEventHandler RegistryEventHandler}
     *            instance, or <code>null</code>.
     * @param portTypes
     *            the list of port types required by the application. Can be an
     *            empty list, but not null.
     * @return the new Ibis instance.
     * 
     * @exception IbisCreationFailedException
     *                is thrown when no Ibis was found that matches the
     *                capabilities required, or a matching Ibis could not be
     *                instantiated for some reason.
     */
    public static Ibis createIbis(IbisCapabilities requiredCapabilities,
            Properties properties, boolean addDefaultConfigProperties,
            RegistryEventHandler registryEventHandler, PortType... portTypes)
            throws IbisCreationFailedException {
        return createIbis(requiredCapabilities, properties,
                addDefaultConfigProperties, registryEventHandler, null,
                (byte[]) null, portTypes);
    }

    /**
     * Creates a new Ibis instance, based on the specified properties,
     * making sure that the Ibis implementation
     * chosen implements the specified capabilities and port types.
     * 
     * @param requiredCapabilities
     *            ibis capabilities required by the application.
     * @param properties
     *            properties that can be set, for instance a class path for
     *            searching ibis implementations, or which registry to use.
     *            There is a default, so <code>null</code> may be specified.
     * @param addDefaultConfigProperties
     *            adds the default properties, loaded from the system
     *            properties, a "ibis.properties" file, etc, for as far as these
     *            are not set in the <code>properties</code> parameter.
     * @param registryEventHandler
     *            a {@link ibis.ipl.RegistryEventHandler RegistryEventHandler}
     *            instance, or <code>null</code>.
     * @param credentials
     *            Credentials used to join the pool. This could be a password, a
     *            certificate, or something else.
     * @param portTypes
     *            the list of port types required by the application. Can be an
     *            empty list, but not null.
     * @return the new Ibis instance.
     * 
     * @exception IbisCreationFailedException
     *                is thrown when the chosen Ibis implementation cannot be
     *                created for some reason.
     */
    public static Ibis createIbis(IbisCapabilities requiredCapabilities,
            Properties properties, boolean addDefaultConfigProperties,
            RegistryEventHandler registryEventHandler, Credentials credentials,
            PortType... portTypes) throws IbisCreationFailedException {
        return createIbis(requiredCapabilities, properties,
                addDefaultConfigProperties, registryEventHandler, credentials,
                (byte[]) null, portTypes);
    }

    /**
     * Creates a new Ibis instance, based on the specified properties,
     * making sure that the Ibis implementation
     * chosen implements the specified capabilities and port types.
     * 
     * @param requiredCapabilities
     *            ibis capabilities required by the application.
     * @param properties
     *            properties that can be set, for instance a class path for
     *            searching ibis implementations, or which registry to use.
     *            There is a default, so <code>null</code> may be specified.
     * @param addDefaultConfigProperties
     *            adds the default properties, loaded from the system
     *            properties, a "ibis.properties" file, etc, for as far as these
     *            are not set in the <code>properties</code> parameter.
     * @param registryEventHandler
     *            a {@link ibis.ipl.RegistryEventHandler RegistryEventHandler}
     *            instance, or <code>null</code>.
     * @param credentials
     *            Credentials used to join the pool. This could be a password, a
     *            certificate, or something else.
     * @param tag
     *            A tag for this Ibis instance.
     * @param portTypes
     *            the list of port types required by the application. Can be an
     *            empty list, but not null.
     * @return the new Ibis instance.
     * 
     * @exception IbisCreationFailedException
     *                is thrown when the chosen Ibis implementation cannot be
     *                created for some reason.
     */
    public static Ibis createIbis(IbisCapabilities requiredCapabilities,
            Properties properties, boolean addDefaultConfigProperties,
            RegistryEventHandler registryEventHandler, Credentials credentials,
            String tag, PortType... portTypes)
            throws IbisCreationFailedException {
        byte[] tagBytes = null;
        if (tag != null) {
            try {
                tagBytes = tag.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IbisCreationFailedException(
                        "could not create tag from string", e);
            }
        }
        return createIbis(requiredCapabilities, properties,
                addDefaultConfigProperties, registryEventHandler, credentials,
                tagBytes, portTypes);
    }

    /**
     * Creates a new Ibis instance, based on the specified properties,
     * making sure that the Ibis implementation
     * chosen implements the specified capabilities and port types.
     * 
     * @param requiredCapabilities
     *            ibis capabilities required by the application.
     * @param properties
     *            properties that can be set, for instance a class path for
     *            searching ibis implementations, or which registry to use.
     *            There is a default, so <code>null</code> may be specified.
     * @param addDefaultConfigProperties
     *            adds the default properties, loaded from the system
     *            properties, a "ibis.properties" file, etc, for as far as these
     *            are not set in the <code>properties</code> parameter.
     * @param registryEventHandler
     *            a {@link ibis.ipl.RegistryEventHandler RegistryEventHandler}
     *            instance, or <code>null</code>.
     * @param credentials
     *            Credentials used to join the pool. This could be a password, a
     *            certificate, or something else.
     * @param tag
     *            A tag for this Ibis instance.
     * @param portTypes
     *            the list of port types required by the application. Can be an
     *            empty list, but not null.
     * @return the new Ibis instance.

     * @exception IbisCreationFailedException
     *                is thrown when the chosen Ibis implementation cannot be
     *                created for some reason.
     */
    @SuppressWarnings("unchecked")
    public static Ibis createIbis(IbisCapabilities requiredCapabilities,
            Properties properties, boolean addDefaultConfigProperties,
            RegistryEventHandler registryEventHandler, Credentials credentials,
            byte[] tag, PortType... portTypes)
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

        // get/create factory
        IbisFactory factory = getFactory(implPath, combinedProperties);

        String specifiedImplementation = combinedProperties
                .getProperty(IbisProperties.IMPLEMENTATION);

        // create the ibis instance
        return factory.createIbis(registryEventHandler, requiredCapabilities,
                combinedProperties, credentials, tag, portTypes,
                specifiedImplementation);
    }

    /**
     * Factory does some initial sanity checks. Port types can only specify a
     * single connection capability, and must specify a serialization.
     */
    private void checkSanity(RegistryEventHandler registryEventHandler,
            IbisCapabilities capabilities, PortType[] portTypes)
            throws IbisConfigurationException {
        for (PortType portType : portTypes) {
            // Check sanity of port types.
            int count = 0;
            if (portType.hasCapability(PortType.CONNECTION_MANY_TO_MANY)) {
                count++;
            }
            if (portType.hasCapability(PortType.CONNECTION_ONE_TO_ONE)) {
                count++;
            }
            if (portType.hasCapability(PortType.CONNECTION_ONE_TO_MANY)) {
                count++;
            }
            if (portType.hasCapability(PortType.CONNECTION_MANY_TO_ONE)) {
                count++;
            }
            if (count != 1) {
                throw new IbisConfigurationException("PortType " + portType
                        + " should specify exactly one connection type");
            }
            String[] strings = portType.getCapabilities();
            boolean serializationSpecified = false;
            for (String s : strings) {
                if (s.startsWith(PortType.SERIALIZATION)) {
                    serializationSpecified = true;
                    break;
                }
            }
            if (!serializationSpecified) {
                throw new IbisConfigurationException("Port type " + portType
                        + " should specify serialization");
            }
        }

        // If a registryEventHandler is specified, the membership capability
        // must be requested as well.

        if (registryEventHandler != null
                && !capabilities
                        .hasCapability(IbisCapabilities.MEMBERSHIP_UNRELIABLE)
                && !capabilities
                        .hasCapability(IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED)) {
            throw new IbisConfigurationException(
                    "RegistryEventHandler specified but no "
                            + " membership capability requested");
        }
    }

    @SuppressWarnings("unchecked")
    /**
     * Creates an ibis. Should only be used by Ibises to create "child" Ibises.
     * Applications should use the static functions to create an Ibis instance.
     */
    public Ibis createIbis(RegistryEventHandler registryEventHandler,
            IbisCapabilities requiredCapabilities, Properties properties,
            Credentials credentials, byte[] applicationTag,
            PortType[] portTypes, String specifiedImplementation)
            throws IbisCreationFailedException {

        if (requiredCapabilities == null) {
            throw new IbisConfigurationException("capabilities not specified");
        }

        if (portTypes == null) {
            throw new IbisConfigurationException("port types not specified");
        }

        // print some info
        if (isVerbose(properties)) {
            System.err
                    .println("Looking for an IPL Implementation with capabilities: "
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

            StringBuffer str = new StringBuffer();
            str.append("IPL implementations:");
            for (IbisStarter starter : implementations.values()) {
                str.append(" ");
                str.append(starter.getNickName());
            }
            System.err.println(str.toString());
        }

        checkSanity(registryEventHandler, requiredCapabilities, portTypes);

        // we allow users to specify implementations as impl1,impl2,impl3 to
        // denote a stack of implementations. We take the first part here,
        // and pass the rest to the ibis we create
        String specifiedSubImplementation = null;
        if (specifiedImplementation != null) {
            String[] parts = specifiedImplementation.split(",", 2);

            specifiedImplementation = parts[0];

            if (parts.length == 2) {
                specifiedSubImplementation = parts[1];
            }
        }

        IbisStarter starter = selectImplementation(requiredCapabilities,
                portTypes, specifiedImplementation);

        if (isVerbose(properties)) {
            System.err.println("Selected ipl implementation: "
                    + starter.getNickName());
        }

        return starter.startIbis(this, registryEventHandler, properties,
                requiredCapabilities, credentials, applicationTag, portTypes,
                specifiedSubImplementation);

    }

    private IbisStarter selectImplementation(
            IbisCapabilities requiredCapabilities, PortType[] portTypes,
            String specifiedImplementation) throws IbisCreationFailedException {

        // The user specified an implementation. Try to find it, and see if it
        // matches the requirements
        if (specifiedImplementation != null) {
            IbisStarter starter = implementations.get(specifiedImplementation);

            if (starter == null) {
                throw new IbisCreationFailedException(
                        "User specified implementation \""
                                + specifiedImplementation
                                + "\" cannot be found");
            }

            if (!starter.matches(requiredCapabilities, portTypes)) {
                CapabilitySet unmatchedCapabilities = starter
                        .unmatchedIbisCapabilities(requiredCapabilities,
                                portTypes);
                PortType[] remainingPortTypes = starter.unmatchedPortTypes(
                        requiredCapabilities, portTypes);

                String portTypeString = "";
                for (PortType portType : remainingPortTypes) {
                    portTypeString = portTypeString + " " + portType;
                }

                throw new IbisCreationFailedException(
                        "User specified implementation \""
                                + specifiedImplementation
                                + "\" does not fulfill specified requirements. Unmatched capabilities = "
                                + unmatchedCapabilities
                                + ", Unmatched port-types = " + portTypeString);
            }

            return starter;
        }

        // auto detect implementation

        // find all matching implementations
        ArrayList<IbisStarter> matchingIbises = new ArrayList<IbisStarter>();
        for (IbisStarter starter : implementations.values()) {
            if (starter.matches(requiredCapabilities, portTypes)) {
                matchingIbises.add(starter);
            }
        }

        // if no implementations match, throw an error
        if (matchingIbises.size() == 0) {
            throw new IbisCreationFailedException(
                    "Cannot find Ibis Implementation matching requirements");
        }

        // if only one implementation matches, use that one
        if (matchingIbises.size() == 1) {
            return matchingIbises.get(0);
        }

        // if multiple implementation match, return the default implementation
        for (IbisStarter starter : matchingIbises) {
            if (starter.getNickName().equals(DEFAULT_IMPLEMENTATION)) {
                return starter;
            }
        }

        // default not found in possible choices, we give up...

        String possibilities = "";
        for (IbisStarter starter : matchingIbises) {
            possibilities = possibilities + " " + starter.getNickName();
        }

        throw new IbisCreationFailedException(
                "Multiple ibis implementations matchs requirements, but the default implementation (\""
                        + DEFAULT_IMPLEMENTATION
                        + "\") is not in list of possibilities: \""
                        + possibilities
                        + "\", please select an ibis manually with the \""
                        + IbisProperties.IMPLEMENTATION + "\" property");
    }

    /**
     * Creates a JarFile object from a given File object.
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

    private static IbisStarter loadIbisFromJar(JarFile jar,
            ClassLoader classLoader) {

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

            return IbisStarter.createInstance(starterClass, classLoader,
                    nickName, iplVersion, implementationVersion);
        } catch (Exception e) {
            System.err.println("Could not load ibis from jar: " + jar.getName()
                    + ": " + e);
            return null;
        }
    }

    private void loadIbisesFromJars(String implementationPath) {
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
            IbisStarter starter = loadIbisFromJar(jarFiles[i], classLoader);

            if (starter != null) {
                implementations.put(starter.getNickName(), starter);
            }
        }
    }

    private void loadIbisesFromManifestFile() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();

            String nickNames = manifestProperties.getProperty("implementations");

            if (nickNames == null) {
                System.err
                        .println("Warning: no implementations found in manifest property file");
            }

            for (String nickName : nickNames.split(",")) {
                if (implementations.containsKey(nickName)) {
                    // we already have this implementation, skip
                    continue;
                }

                String iplVersion = manifestProperties.getProperty(nickName
                        + ".ipl.version", null);
                String implementationVersion = manifestProperties.getProperty(nickName
                        + ".version", null);
                String starterClass = manifestProperties.getProperty(nickName
                        + ".starter.class", null);

                if (iplVersion == null || !iplVersion.startsWith(VERSION)
                        || implementationVersion == null || nickName == null
                        || starterClass == null) {
                    continue;
                }

                IbisStarter starter = IbisStarter.createInstance(starterClass,
                        classLoader, nickName, iplVersion,
                        implementationVersion);

                if (starter != null) {
                    implementations.put(nickName, starter);
                }
            }
        } catch (Throwable t) {
            System.err
                    .println("Warning: could not load implementation from manifest property file: "
                            + t);
            t.printStackTrace(System.err);
        }
    }
}
