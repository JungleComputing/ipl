/* $Id: IbisFactory.java 4910 2006-12-13 09:01:33Z ceriel $ */

package ibis.ipl;

import ibis.util.ClassLister;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

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
 * "properties" should be present in the package of this Ibis implementation,
 * and describe the specific properties of this Ibis implementation.
 */

public final class IbisFactory {

    private static final String ldpath = "ibis.library.path";

    private static final String implpath = "ibis.impl.path";

    private static final String[] sysprops = { ldpath, implpath };

    private static final String[] excludes = { "ibis.util.", "ibis.connect.",
            "ibis.pool.", "ibis.io.", "ibis.net.", "ibis.mp.", "ibis.nio.",
            "ibis.tcp.", "ibis.name_server.", "ibis.name", "ibis.verbose",
            "ibis.communication", "ibis.serialization", "ibis.worldmodel" };

    private static final String implPathValue
        = System.getProperty(implpath);

    /** A list of available ibis implementations. */
    private static Class[] implList;

    /** Properties of available ibis implementations. */
    private static StaticProperties[] implProperties;

    /** The currently loaded Ibises. */
    private static ArrayList loadedIbises = new ArrayList();

    static {
        // Check properties
        checkProperties("ibis.", sysprops, excludes);

        // Obtain a list of Ibis implementations
        ClassLister clstr = ClassLister.getClassLister(implPathValue);
        List compnts = clstr.getClassList("Ibis-Implementation", Ibis.class);
        implList = (Class[]) compnts.toArray(new Class[0]);
        implProperties = new StaticProperties[implList.length];
        for (int i = 0; i < implProperties.length; i++) {
            try {
                addIbis(i);
            } catch(IOException e) {
                System.err.println("Error while reading properties of "
                        + implList[i].getName() + ": " + e);
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Private constructor prevents creation.
     */
    private IbisFactory() {
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
    public static void loadLibrary(String name) throws SecurityException,
            UnsatisfiedLinkError {
        Properties p = System.getProperties();
        String libPath = p.getProperty(ldpath);
        String sep = p.getProperty("file.separator");

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

    private static Ibis createIbis(Class c,
            StaticProperties prop, StaticProperties reqprop,
            ResizeHandler resizeHandler) throws IOException {

        Ibis impl;

        try {
            impl = (Ibis) c.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Could not initialize Ibis" + e);
        } catch (IllegalAccessException e2) {
            throw new IllegalArgumentException("Could not initialize Ibis"
                                               + e2);
        }

        try {
            loadLibrary("uninitialized_object");
        } catch (Throwable t) {
            /* handled elsewhere */
        }

        if (reqprop == null) {
            reqprop = staticProperties(c.getName());
        } else if (reqprop.isProp("serialization", "object")) {
            /*
             * required properties had "object", but if we later
             * ask for "sun" or "ibis", these may not be in the
             * required properties, so put the original serialization
             * specs back.
             */
            reqprop = new StaticProperties(reqprop);
            reqprop.add("serialization",
                    staticProperties(c.getName()).find("serialization"));
        }
        if (prop == null) {
            prop = reqprop.combineWithUserProps();
        }

        impl.init(resizeHandler, reqprop, prop);

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
     * Creates a new Ibis instance, based on the required properties,
     * or on the system property "ibis.name", or on the staticproperty "name".
     * If the system property "ibis.name" is set, the corresponding
     * Ibis implementation is chosen.
     * Else, if the staticproperty "name" is set in the specified
     * required properties, the corresponding Ibis implementation is chosen.
     * Else, an Ibis implementation is chosen that matches the
     * required properties.
     *
     * @param reqprop static properties required by the application,
     *  or <code>null</code>.
     * @param  r a {@link ibis.ipl.ResizeHandler ResizeHandler} instance
     *  if upcalls for joining or leaving ibis instances are required,
     *  or <code>null</code>.
     * @return the new Ibis instance.
     *
     * @exception NoMatchingIbisException is thrown when no Ibis was
     *  found that matches the properties required.
     * @exception NextedException is thrown when no Ibis could be
     *  instantiated.
     */
    public static Ibis createIbis(StaticProperties reqprop, ResizeHandler r)
            throws NoMatchingIbisException, NestedException {

        StaticProperties combinedprops;

        if (reqprop == null) {
            combinedprops = (new StaticProperties()).combineWithUserProps();
        } else {
            combinedprops = reqprop.combineWithUserProps();
        }

        if (combinedprops.find("verbose") != null) {
            System.out.println("Looking for an Ibis with properties: ");
            System.out.println("" + combinedprops);
        }

        String ibisname = combinedprops.find("name");

        ArrayList implementations = new ArrayList();

        if (ibisname == null) {
            NestedException nested = new NestedException(
                    "Could not find a matching Ibis");
            for (int i = 0; i < implProperties.length; i++) {
                StaticProperties ibissp = implProperties[i];
                Class cl = implList[i];
                // System.out.println("try " + cl.getName());
                if (combinedprops.matchProperties(ibissp)) {
                    // System.out.println("match!");
                    implementations.add(cl);
                }
                StaticProperties clashes
                        = combinedprops.unmatchedProperties(ibissp);
                nested.add(cl.getName(),
                        new Exception("Unmatched properties: "
                            + clashes.toString()));
            }
            if (implementations.size() == 0) {
                // System.err.println("Properties:");
                // System.err.println(combinedprops.toString());
                throw new NoMatchingIbisException(nested);
            }
        } else {
            StaticProperties ibissp = null;
            Class cl = null;
            boolean found = false;
            for (int i = 0; i < implProperties.length; i++) {
                ibissp = implProperties[i];
                cl = implList[i];

                String name = ibisname;
                if (name.startsWith("net")) {
                    name = "net";
                }
                String n = ibissp.getProperty("nickname");
                if (n == null) {
                    n = cl.getName().toLowerCase();
                }

                if (name.equals(n) || name.equals(cl.getName().toLowerCase())) {
                    found = true;
                    implementations.add(cl);
                    break;
                }
            }

            if (! found) {
                throw new NoMatchingIbisException("Nickname " + ibisname + " not matched");
            }

            if (!combinedprops.matchProperties(ibissp)) {
                StaticProperties clashes
                        = combinedprops.unmatchedProperties(ibissp);
                System.err.println("WARNING: the " + ibisname
                       + " version of Ibis does not match the required "
                       + "properties.\nThe unsupported properties are:\n"
                       + clashes.toString()
                       + "This Ibis version was explicitly requested, "
                       + "so the run continues ...");
            }
            if (ibisname.startsWith("net")) {
                ibissp.add("IbisName", ibisname);
            }
        }

        int n = implementations.size();

        if (combinedprops.find("verbose") != null) {
            System.out.print("Matching Ibis implementations:");
            for (int i = 0; i < n; i++) {
                Class cl = (Class) implementations.get(i);
                System.out.print(" " + cl.getName());
            }
            System.out.println();
        }

        NestedException nested = new NestedException("Ibis creation failed");
        
        for (int i = 0; i < n; i++) {
            Class cl = (Class) implementations.get(i);
            if (combinedprops.find("verbose") != null) {
                System.out.println("trying " + cl.getName());
            }
            while (true) {
                try {
                    return createIbis(cl, combinedprops, reqprop, r);
                } catch (ConnectionRefusedException e) {
                    // retry
                } catch (Throwable e) {
                    nested.add(cl.getName(), e);
                    if (i == n - 1) {
                        // No more Ibis to try.
                        throw nested;
                    }

                    if (combinedprops.find("verbose") != null) {
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

    private static void addIbis(int index) throws IOException {
        Class cl = implList[index];
        String packagename = cl.getPackage().getName();
        // Note: getResourceAsStream wants '/', not File.separatorChar!
        String propertyFile = packagename.replace('.', '/')
                    + "/" + "properties";
        StaticProperties sp = StaticProperties.load(propertyFile);
        implProperties[index] = sp;
    }

    /**
     * Returns the static properties for a certain implementation.
     * @param implName implementation name of an Ibis for which
     * properties are requested.
     * @return the static properties for a given implementation,
     *  or <code>null</code> if not present.
     */
    public static synchronized StaticProperties staticProperties(
            String implName) {
        for (int i = 0; i < implList.length; i++) {
            if (implList[i].getName().equals(implName)) {
                return implProperties[i];
            }
        }
        return null;
    }

    /**
     * Check validity of a System property.
     * All system properties are checked; when the name starts with the
     * specified prefix, it should be in the specified list of property names,
     * unless it starts with one of the exclude members.
     * If the property is not found, a warning is printed.
     *
     * @param prefix prefix of checked property names, for instance "satin.".
     * @param propnames list of accepted property names.
     * @param excludes list of property prefixes that should not be checked.
     */
    private static void checkProperties(String prefix, String[] propnames,
            String[] excludes) {
        Properties p = System.getProperties();
        for (Enumeration e = p.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            if (name.startsWith(prefix)) {
                boolean found = false;
                if (excludes != null) {
                    for (int i = 0; i < excludes.length; i++) {
                        if (name.startsWith(excludes[i])) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    if (propnames != null) {
                        for (int i = 0; i < propnames.length; i++) {
                            if (name.equals(propnames[i])) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
                if (!found) {
                    System.err.println("Warning: property \"" + name
                            + "\" has prefix \"" + prefix
                            + "\" but is not recognized");
                }
            }
        }
    }
}
