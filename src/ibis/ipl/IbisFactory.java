/* $Id$ */

package ibis.ipl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This is the class responsible for starting an Ibis instance.
 * During initialization, this class determines which Ibis implementations
 * are available. It does so, by finding all jar files in either the
 * class path, or all jar files in the directories indicated by the
 * ibis.impl.path property.
 * All Ibis implementations should be mentioned in the main
 * attributes of the manifest of the jar file containing it, in the
 * "Ibis-Implementation" entry. This entry should contain a
 * comma- or space-separated list of class names, where each class named
 * provides an Ibis implementation. In addition, a jar-entry named
 * "capabilities" should be present in the package of this Ibis implementation,
 * and describe the specific capabilities of this Ibis implementation.
 */

public final class IbisFactory implements PredefinedCapabilities {

    private static final String[] excludes = { "util.", "connect.", "pool.",
            "library.",
            "io.", "impl.", "registry.", "name", "verbose", "serialization" };

    private Class[] implList;

    private CapabilitySet[] capsList;

    /** The currently loaded Ibises. */
    private static ArrayList loadedIbises = new ArrayList();

    private static IbisFactory defaultFactory = new IbisFactory(
            IbisAttributes.getDefaultAttributes());

    private TypedProperties attribs;

    /**
     * Private constructor prevents creation from outside.
     */
    private IbisFactory(TypedProperties attribs) {
        this.attribs = attribs;
        // Check capabilities
        attribs.checkProperties(IbisAttributes.PREFIX, new String[] {},
                excludes, true);
        // Obtain a list of Ibis implementations
        String implPathValue = attribs.getProperty(IbisAttributes.IMPLPATH);
        ClassLister clstr = ClassLister.getClassLister(implPathValue);
        List compnts = clstr.getClassList("Ibis-Implementation", Ibis.class);
        implList = (Class[]) compnts.toArray(new Class[0]);
        capsList = new CapabilitySet[implList.length];
        for (int i = 0; i < capsList.length; i++) {
            try {
                addIbis(i);
            } catch(IOException e) {
                System.err.println("Error while reading capabilities of "
                        + implList[i].getName() + ": " + e);
                e.printStackTrace();
                System.exit(1);
            }
        }
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
     * @exception SecurityException may be thrown by loadLibrary.
     * @exception UnsatisfiedLinkError may be thrown by loadLibrary.
     */
    public void loadLibrary(String name) throws SecurityException,
            UnsatisfiedLinkError {
        String libPath = attribs.getProperty(IbisAttributes.LDPATH);
        String sep = System.getProperty("file.separator");

        if (libPath != null) {
            String s = System.mapLibraryName(name);

            // System.err.println("LOADING IBIS LIB: " + libPath + sep + s);

            System.load(libPath + sep + s);
            return;
        }

        // Fall back to regular loading.
        // This might not work, or it might not :-)
        // System.err.println("LOADING NON IBIS LIB: " + name);

        System.loadLibrary(name);
    }

    private Ibis createIbis(Class c, CapabilitySet caps,
            ResizeHandler resizeHandler) throws Throwable {
        Ibis impl;

        try {
            impl = (Ibis) c.getConstructor(new Class[] {ResizeHandler.class,
                    CapabilitySet.class, TypedProperties.class}).newInstance(
                        new Object[] {resizeHandler, caps, attribs});
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }

        synchronized (Ibis.class) {
            loadedIbises.add(impl);
        }
        return impl;
    }

    /**
     * Returns a list of all Ibis implementations that are currently loaded.
     * When no Ibises are loaded, this method returns an array with no
     * elements.
     * @return the list of loaded Ibis implementations.
     */
    public static synchronized Ibis[] loadedIbises() {
        Ibis[] res = new Ibis[loadedIbises.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = (Ibis) loadedIbises.get(i);
        }

        return res;
    }

    /**
     * Creates a new Ibis instance, based on the required capabilities,
     * and using the specified attributes.
     *
     * @param requiredCapabilities capabilities required by the application.
     * @param optionalCapabilities capabilities that might come in handy, or
     * <code>null</code>.
     * @param attributes properties that can be set, for instance
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
            CapabilitySet optionalCapabilities, TypedProperties attributes,
            ResizeHandler r) throws NoMatchingIbisException, NestedException {

        IbisFactory fac;

        if (attributes == null) {
            fac = defaultFactory;
        } else {
            TypedProperties tp =
                    new TypedProperties(IbisAttributes.getDefaultAttributes());
            tp.putAll(attributes);
            fac = new IbisFactory(tp);
        }

        return fac.createIbis(requiredCapabilities, optionalCapabilities, r);
    }

    private Ibis createIbis(CapabilitySet requiredCapabilities,
            CapabilitySet optionalCapabilities, ResizeHandler r)
            throws NoMatchingIbisException, NestedException {

        if (attribs.getProperty("ibis.verbose") != null) {
            System.out.println("Looking for an Ibis with capabilities: ");
            System.out.println("" + requiredCapabilities);
        }

        CapabilitySet total;
        if (optionalCapabilities != null) {
            total = requiredCapabilities.uniteWith(optionalCapabilities);
        } else {
            total = requiredCapabilities;
        }

        if (total.hasCapability(WORLD_OPEN) &&
            total.hasCapability(WORLD_CLOSED)) {
            throw new IbisConfigurationException("It is not allowed to ask for "
                    + "open world as well as closed world");
        }

        String ibisname = attribs.getProperty("ibis.name");

        ArrayList<Class> implementations = new ArrayList<Class>();

        ArrayList<CapabilitySet> caps = new ArrayList<CapabilitySet>();

        if (ibisname == null) {
            NestedException nested = new NestedException(
                    "Could not find a matching Ibis");
            for (int i = 0; i < capsList.length; i++) {
                CapabilitySet ibissp = capsList[i];
                Class cl = implList[i];
                // System.out.println("try " + cl.getName());
                if (requiredCapabilities.matchCapabilities(ibissp)) {
                    // System.out.println("match!");
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
                // System.err.println("Capabilities:");
                // System.err.println(requiredCapabilities.toString());
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
                System.err.println("WARNING: the " + ibisname
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

        if (attribs.getProperty("ibis.verbose") != null) {
            System.out.print("Matching Ibis implementations:");
            for (int i = 0; i < n; i++) {
                Class cl = implementations.get(i);
                System.out.print(" " + cl.getName());
            }
            System.out.println();
        }

        NestedException nested = new NestedException("Ibis creation failed");
        
        for (int i = 0; i < n; i++) {
            Class cl = (Class) implementations.get(i);
            if (attribs.getProperty("ibis.verbose") != null) {
                System.out.println("trying " + cl.getName());
            }
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

                    if (attribs.getProperty("ibis.verbose") != null) {
                        System.err.println("Warning: could not create "
                                + cl.getName() + ", got exception:" + e);
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        throw nested;
    }

    private void addIbis(int index) throws IOException {
        Class cl = implList[index];
        String packagename = cl.getPackage().getName();
        // Note: getResourceAsStream wants '/', not File.separatorChar!
        String capabilityFile = packagename.replace('.', '/')
                    + "/" + "capabilities";
        capsList[index] = CapabilitySet.load(capabilityFile);
    }

    /**
     * This class exports a method for searching either the classpath or a
     * specified list of directories for jar-files with a specified name in
     * the Manifest.
     * This is a copy of ibis.util.ClassLister, but included here, because we
     * don't want a dependency on ibis.util here. ibis.ipl should be the only
     * package needed in the classpath. The rest should be brought in by the
     * URLClassLoader.
     */
    private static class ClassLister {

        private JarFile[] jarFiles;

        private ClassLoader ld = null;

        private static HashMap<String, ClassLister> listers
                = new HashMap<String, ClassLister>();

        private static ClassLister classPathLister = null;

        /**
         * Constructs a <code>ClassLister</code> from the specified directory
         * list. All jar files found in the specified directories are used.
         * if <code>dirList</code> is <code>null</code>, all jar files from the
         * classpath are used instead.
         * @param dirList a list of directories, or <code>null</code>, in which
         * the classpath is used to find jar files.
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
                    urls[i] = f.toURL();
                } catch (Exception e) {
                    throw new Error(e);
                }
            }

            ld = new URLClassLoader(urls, this.getClass().getClassLoader());
        }

        /**
         * Obtains a <code>ClassLister</code> for the specified directory
         * list. All jar files found in the specified directories are used.
         * if <code>dirList</code> is <code>null</code>, all jar files from the
         * classpath are used instead.
         * @param dirList a list of directories, or <code>null</code>, in which
         * the classpath is used to find jar files.
         * @return the required <code>ClassLister</code>.
         */
        public static synchronized ClassLister getClassLister(String dirList) {
            if (dirList == null) {
                if (classPathLister == null) {
                    classPathLister = new ClassLister(dirList);
                }
                return classPathLister;
            }

            ClassLister lister = (ClassLister) listers.get(dirList);
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
                StringTokenizer st = new StringTokenizer(classPath,
                        File.pathSeparator);
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
                    } catch(IOException e) {
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
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    try {
                        JarFile jarFile = new JarFile(files[i], true);
                        Manifest manifest = jarFile.getManifest();
                        if (manifest != null) {
                            manifest.getMainAttributes();
                            jarList.add(jarFile);
                        }
                    } catch(IOException e) {
                        // ignore
                    }
                }
            }
        }

        /**
         * This method reads all jar files found in the specified directories,
         * and stores them in a list that can be searched for specific names
         * later on.
         * @param dirList list of directories to search, separator is
         * <code>java.io.File.pathSeparator</code>.
         */
        protected void readJarFiles(String dirList) {
            ArrayList<JarFile> jarList = new ArrayList<JarFile>();

            StringTokenizer st
                    = new StringTokenizer(dirList, File.pathSeparator);

            while (st.hasMoreTokens()) {
                String dir = st.nextToken();
                addJarFiles(dir, jarList);
            }
            jarFiles = jarList.toArray(new JarFile[0]);
        }

        /**
         * Returns a list of classes for the specified attribute name.
         * The specified manifest attribute name is assumed to be
         * mapped to a comma-separated list of class names.
         * All jar files in the classpath
         * are scanned for the specified manifest attribute name, and
         * the attribute values are loaded.
         * @param attribName the manifest attribute name.
         * @return the list of classes.
         */
        public List<Class> getClassList(String attribName) {
            ArrayList<Class> list = new ArrayList<Class>();

            for (int i = 0; i < jarFiles.length; i++) {
                Manifest mf = null;
                try {
                    mf = jarFiles[i].getManifest();
                } catch(IOException e) {
                    throw new Error("Could not get Manifest from "
                            + jarFiles[i].getName(), e);
                }
                if (mf != null) {
                    Attributes ab = mf.getMainAttributes();
                    String classNames = ab.getValue(attribName);
                    if (classNames != null) {
                        StringTokenizer st
                                = new StringTokenizer(classNames, ", ");
                        while (st.hasMoreTokens()) {
                            String className = st.nextToken();
                            try {
                                Class cl = Class.forName(className, false, ld);
                                list.add(cl);
                            } catch(Exception e) {
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
         * Returns a list of classes for the specified attribute name.
         * The specified manifest attribute name is assumed to be
         * mapped to a comma-separated list of class names.
         * All jar files in the classpath
         * are scanned for the specified manifest attribute name, and
         * the attribute values are loaded.
         * The classes thus obtained should be extensions of the specified
         * class, or, if it is an interface, implementations of it.
         * @param attribName the manifest attribute name.
         * @param clazz the class of which the returned classes are
         *    implementations or extensions.       
         * @return the list of classes.
         */
        public List<Class> getClassList(String attribName, Class<?> clazz) {
            List<Class> list = getClassList(attribName);

            for (Iterator<Class> iter = list.iterator(); iter.hasNext();) {
                Class<?> cl = iter.next();
                if (! clazz.isAssignableFrom(cl)) {
                    throw new Error("Class " + cl.getName()
                            + " cannot be assigned to class "
                            + clazz.getName());
                }
            }
            return list;
        }
    }
}
