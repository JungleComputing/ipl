/* $Id$ */

package ibis.ipl;

import ibis.ipl.IbisStarter.IbisStarterInfo;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    
    private static final String IPL_VERSION_STRING = "Ibis-IPL-Version";
    private static final String STARTER_STRING = "Ibis-Starter";
    private static final String IMPLEMENTATION_VERSION_STRING = "Ibis-Implementation-Version";
    
    // Map of factories. One for each implementation path
    private static final Map<String, IbisFactory> factories =
        new HashMap<String, IbisFactory>();

    private static IbisFactory defaultFactory;

    static final String VERSION = "2.0";

    
    private static synchronized IbisFactory getFactory(String implPath, Properties properties) {
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

    private IbisStarterInfo[] implList;

    private boolean verbose = false;

    private String ibisName = null;

    /**
     * Constructs an Ibis factory, with the specified search path.
     * 
     * @param implPath
     *            the path to search for implementations.
     */
    @SuppressWarnings("unchecked")
    private IbisFactory(String implPath, Properties properties) {
        // Obtain a list of Ibis implementations
        ClassLister clstr = ClassLister.getClassLister(implPath);
        List<IbisStarterInfo> compnts =
            clstr.getClassList(STARTER_STRING, IbisStarter.class, VERSION);
                       
        // If we found no starters then see if we have starters of last resort
        if (compnts.size() == 0) {
            String startersOfLastResort = properties.getProperty(IbisProperties.IBIS_STARTERS);
            ArrayList<IbisStarterInfo> foundStarters = new ArrayList<IbisStarterInfo>();
            if (startersOfLastResort != null) {
                StringTokenizer st =
                    new StringTokenizer(startersOfLastResort, ", ");
                while (st.hasMoreTokens()) {
                    String starterClassName = st.nextToken();
                        try {
                            IbisStarterInfo starterClass = new IbisStarterInfo(
                                        (Class<? extends IbisStarter>) Class.forName(starterClassName), "");
                            foundStarters.add(starterClass);
                        }
                        catch (ClassNotFoundException e) {
                            // Error will be thrown later if nothing is found
                        }
                }
                // Finish the setup based on these found starters
            }
            implList = foundStarters.toArray(new IbisStarterInfo[foundStarters.size()]);
        } else {
            implList = compnts.toArray(new IbisStarterInfo[compnts.size()]);
        }
        if (implList.length == 0) {
            throw new IbisConfigurationException("Cannot find any Ibis implementations");
        }
    }

    /**
     * Creates a new Ibis instance, based on the required capabilities and port
     * types. As the set of properties, the default properties are used.
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
     *                is thrown when no Ibis was found that matches the
     *                capabilities required, or a matching Ibis could not be
     *                instantiated for some reason.
     */
    public static Ibis createIbis(IbisCapabilities requiredCapabilities,
            RegistryEventHandler registryEventHandler, PortType... portTypes)
            throws IbisCreationFailedException {
        return createIbis(requiredCapabilities, null, true,
            registryEventHandler, portTypes);
    }

    /**
     * Creates a new Ibis instance, based on the required capabilities and port
     * types, and using the specified properties.
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
     *            the list of port types required by the application.
     * @return the new Ibis instance.
     * 
     * @exception IbisCreationFailedException
     *                is thrown when no Ibis was found that matches the
     *                capabilities required, or a matching Ibis could not be
     *                instantiated for some reason.
     */
    @SuppressWarnings("unchecked")
    public static Ibis createIbis(IbisCapabilities requiredCapabilities,
            Properties properties, boolean addDefaultConfigProperties,
            RegistryEventHandler registryEventHandler, PortType... portTypes)
            throws IbisCreationFailedException {

        Properties combinedProperties = new Properties();

        // add default properties, if required
        if (addDefaultConfigProperties) {
            Properties defaults = IbisProperties.getDefaultProperties();

            for (Enumeration<String> e = (Enumeration<String>)defaults.propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement();
                String value = defaults.getProperty(key);
                combinedProperties.setProperty(key, value);
            }
        }

        // add user properties
        if (properties != null) {
            for (Enumeration<String> e = (Enumeration<String>)properties.propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement();
                String value = properties.getProperty(key);
                combinedProperties.setProperty(key, value);
            }
        }

        String implPath =
            combinedProperties.getProperty(IbisProperties.IMPLEMENTATION_PATH);
        IbisFactory factory = getFactory(implPath, combinedProperties);

        return factory.createIbis(registryEventHandler, requiredCapabilities,
            combinedProperties, portTypes);
    }

    private List<IbisStarter> findIbisStack(IbisCapabilities capabilities,
            PortType[] portTypes, List<IbisStarter> selected, String ibisName,
            IbisCreationFailedException creationException) {

        IbisCapabilities caps = capabilities;
        PortType[] types = portTypes;
        boolean faulty = false;
        
        ArrayList<IbisStarter> starters = new ArrayList<IbisStarter>();
        
        for (IbisStarterInfo info : implList) {
            if (verbose) {
                System.err.println("Instantiating " + info.getName());
            }

            // Try to instantiate the starter.
            try {
                starters.add(info.createInstance(caps, types));
            } catch (Throwable e) {
                faulty = true;
                // Oops, could not instantiate starter.
                if (creationException != null) {
                    creationException.add(info.getName(), e);
                }
                if (verbose) {
                    System.err.println("Could not instantiate "
                            + info.getName() + ": " + e);
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
                    System.err.println("Matching with " + instance.starterInfo.getName());
                }
                if (instance.matches()) {
                    selected.add(instance);
                    if (verbose) {
                        System.err.println("Class " + instance.starterInfo.getName() + " selected");
                    }
                    return selected;
                }
                // Find out why it did not match.
                String unmatchedCapabilities =
                    instance.unmatchedIbisCapabilities().toString();
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
                    System.err.println("Class " + instance.getClass().getName() + " selected");
                }
                List<IbisStarter> newList
                    = new ArrayList<IbisStarter>(selected);
                newList.add(instance);
                newList = findIbisStack(new IbisCapabilities(
                        instance.unmatchedIbisCapabilities()),
                        instance.unmatchedPortTypes(),
                        newList, null, null);
                if (newList != null) {
                    return newList;
                }
                if (creationException != null) {
                    creationException.add(instance.getClass().toString(),
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
            PortType[] portTypes) throws IbisCreationFailedException {

        if (requiredCapabilities == null) {
            throw new IbisConfigurationException("capabilities not specified");
        }
        
        String verboseValue = properties.getProperty(IbisProperties.VERBOSE);
        // see if the user specified "verbose"
        verbose =
            verboseValue != null
                    && (verboseValue.equals("1") || verboseValue.equals("on")
                            || verboseValue.equals("")
                            || verboseValue.equals("true") || verboseValue.equals("yes"));

        if (verbose) {
            System.err.println("Looking for an Ibis with capabilities: "
                    + requiredCapabilities);
            System.err.println("(ibis) Properties:");
            for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                if (key.startsWith("ibis")) {
                    String value = properties.getProperty(key);
                    System.err.println(key + " = " + value);
                }
            }
        }

        ibisName = properties.getProperty(IbisProperties.IMPLEMENTATION);

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
            for (int i = 0; i < implList.length; i++) {
                str.append(" ");
                str.append(implList[i].getName());
            }
            System.err.println(str.toString());
        }

        IbisCreationFailedException creationException =
            new IbisCreationFailedException("Ibis creation failed");

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
                creationException.add("Ibis factory",
                    new IbisConfigurationException("PortType " + tp
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
                && !requiredCapabilities.hasCapability(IbisCapabilities.MEMBERSHIP_UNRELIABLE)
                && !requiredCapabilities.hasCapability(IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED)) {
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

        List<IbisStarter> stack =
            findIbisStack(requiredCapabilities, portTypes,
                new ArrayList<IbisStarter>(), ibisName, creationException);

        if (stack == null) {
            creationException.add("Ibis factory",
                new IbisConfigurationException("No matching Ibis found"));
            throw creationException;
        }

        IbisStarter starter = stack.remove(0);
        
        try {
            return starter.startIbis(stack, registryEventHandler, 
                    properties,starter.starterInfo.getVersion());
        } catch(Throwable e) {
            creationException.add("" + starter.starterInfo.getName()
                    + " gave exception ", e);
            throw creationException;
        }
    }
    
    /**
     * A helper class to compare file names, so that they can be sorted,
     * and the order becomes predictable and reproducable.
     */
    private static class FileComparator implements Comparator<File> {
        public int compare(File f1, File f2) {
            return f1.getName().compareTo(f2.getName());
        }
    }

    /**
     * This class exports a method for searching either the classpath or a
     * specified list of directories for jar-files with a specified name in the
     * Manifest.
     */
    private static class ClassLister {

        private JarFile[] jarFiles;

        private ClassLoader ld = null;

        private static HashMap<String, ClassLister> listers =
            new HashMap<String, ClassLister>();

        private static ClassLister classPathLister = null;

        /**
         * Constructs a <code>ClassLister</code> from the specified directory
         * list. All jar files found in the specified directories are used. if
         * <code>dirList</code> is <code>null</code>, all jar files from
         * the classpath are used instead.
         * 
         * @param dirList
         *            a list of directories, or <code>null</code>, in which
         *            the classpath is used to find jar files.
         */
        private ClassLister(String dirList) {
            if (dirList != null) {
                readJarFiles(dirList);
            } else {
                readJarFiles();
            }

            URL[] urls = new URL[jarFiles.length];

            for (int i = 0; i < jarFiles.length; i++) {
                try {
                    File f = new File(jarFiles[i].getName());
                    urls[i] = f.toURI().toURL();
                } catch (Exception e) {
                    throw new Error(e);
                }
            }

            ld = new URLClassLoader(urls, this.getClass().getClassLoader());
        }

        /**
         * Obtains a <code>ClassLister</code> for the specified directory
         * list. All jar files found in the specified directories are used. if
         * <code>dirList</code> is <code>null</code>, all jar files from
         * the classpath are used instead.
         * 
         * @param dirList
         *            a list of directories, or <code>null</code>, in which
         *            the classpath is used to find jar files.
         * @return the required <code>ClassLister</code>.
         */
        private static synchronized ClassLister getClassLister(String dirList) {
            if (dirList == null) {
                if (classPathLister == null) {
                    classPathLister = new ClassLister(null);
                }
                return classPathLister;
            }

            ClassLister lister = listers.get(dirList);
            if (lister == null) {
                lister = new ClassLister(dirList);
                listers.put(dirList, lister);
            }
            return lister;
        }

        /**
         * This method reads all jar files from the classpath, and stores them
         * in a list that can be searched for specific names later on.
         */
        protected void readJarFiles() {
            ArrayList<JarFile> jarList = new ArrayList<JarFile>();
            String classPath = System.getProperty("java.class.path");
            if (classPath != null) {
                StringTokenizer st =
                    new StringTokenizer(classPath, File.pathSeparator);
                while (st.hasMoreTokens()) {
                    String jar = st.nextToken();
                    File f = new File(jar);
                    try {
                        JarFile jarFile = new JarFile(f, true);
                        Manifest manifest = jarFile.getManifest();
                        if (manifest != null) {
                            manifest.getMainAttributes();
                            jarList.add(jarFile);
                        }
                    } catch (IOException e) {
                        // ignore. Could be a directory.
                    }
                }
            }
            jarFiles = jarList.toArray(new JarFile[0]);
        }

        private void addJarFiles(String dir, ArrayList<JarFile> jarList) {
            File f = new File(dir);
            File[] files = f.listFiles();
            if (files == null) {
                return;
            }
            // Sort files alphabetically, so that result is reproducable.
            Arrays.sort(files, new FileComparator());
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    try {
                        JarFile jarFile = new JarFile(files[i], true);
                        Manifest manifest = jarFile.getManifest();
                        if (manifest != null) {
                            manifest.getMainAttributes();
                            jarList.add(jarFile);
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }

        /**
         * This method reads all jar files found in the specified directories,
         * and stores them in a list that can be searched for specific names
         * later on.
         * 
         * @param dirList
         *            list of directories to search, separator is
         *            <code>java.io.File.pathSeparator</code>.
         */
        protected void readJarFiles(String dirList) {
            ArrayList<JarFile> jarList = new ArrayList<JarFile>();

            StringTokenizer st =
                new StringTokenizer(dirList, File.pathSeparator);

            while (st.hasMoreTokens()) {
                String dir = st.nextToken();
                addJarFiles(dir, jarList);
            }
            jarFiles = jarList.toArray(new JarFile[0]);
        }

        /**
         * Returns a list of classes for the specified attribute name. The
         * specified manifest attribute name is assumed to be mapped to a
         * comma-separated list of class names. All jar files in the classpath
         * are scanned for the specified manifest attribute name, and the
         * attribute values are loaded.
         * 
         * @param attribName
         *            the manifest attribute name.
         * @param version
         *            required version, or null.
         * @return the list of classes.
         */
        @SuppressWarnings("unchecked")
	private List<IbisStarterInfo> getClassList(String attribName, String version) {
            ArrayList<IbisStarterInfo> list = new ArrayList<IbisStarterInfo>();

            for (int i = 0; i < jarFiles.length; i++) {
                Manifest mf = null;
                try {
                    mf = jarFiles[i].getManifest();
                } catch (IOException e) {
                    throw new Error("Could not get Manifest from "
                            + jarFiles[i].getName(), e);
                }
                if (mf != null) {
                    Attributes ab = mf.getMainAttributes();
                    if (version != null) {
                        String jarVersion = ab.getValue(IPL_VERSION_STRING);
                        if (jarVersion == null
                                || !jarVersion.startsWith(version)) {
                            continue;
                        }
                    }
                    String classNames = ab.getValue(attribName);
                    String ibisVersion = ab.getValue(IMPLEMENTATION_VERSION_STRING);
                    if (ibisVersion == null) {
                        continue;
                    }
                    if (classNames != null) {
                        StringTokenizer st =
                            new StringTokenizer(classNames, ", ");
                        while (st.hasMoreTokens()) {
                            String className = st.nextToken();
                            try {
                                IbisStarterInfo cl = new IbisStarterInfo(
                                        (Class<? extends IbisStarter>) Class.forName(className,
                                                false, ld), ibisVersion);
                                list.add(cl);
                            } catch (Exception e) {
                                throw new Error("Could not load class "
                                        + className
                                        + ". Something wrong with jar "
                                        + jarFiles[i].getName() + "?", e);
                            }
                        }
                    }
                }
            }
            return list;
        }

        /**
         * Returns a list of classes for the specified attribute name. The
         * specified manifest attribute name is assumed to be mapped to a
         * comma-separated list of class names. All jar files in the classpath
         * are scanned for the specified manifest attribute name, and the
         * attribute values are loaded. The classes thus obtained should be
         * extensions of the specified class, or, if it is an interface,
         * implementations of it.
         * 
         * @param attribName
         *            the manifest attribute name.
         * @param clazz
         *            the class of which the returned classes are
         *            implementations or extensions.
         * @param version
         *            required version, or null.
         * @return the list of classes.
         */
        private List<IbisStarterInfo> getClassList(String attribName,
                Class<? extends IbisStarter> clazz, String version) {
            List<IbisStarterInfo> list = getClassList(attribName, version);

            for (IbisStarterInfo cl : list) {
                if (!clazz.isAssignableFrom(cl.getClazz())) {
                    throw new Error("Class " + cl.getName()
                            + " cannot be assigned to class " + clazz.getName());
                }
            }
            return list;
        }
    }
}
