package ibis.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This class exports a method for searching either the classpath or a
 * specified list of directories for jar-files with a specified name in the
 * Manifest.
 */
public class ClassLister {

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
                urls[i] = f.toURI().toURL();
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
     * and stores them in a list that can be searched for specific names later
     * on.
     * @param dirList list of directories to search, separator is
     * <code>java.io.File.pathSeparator</code>.
     */
    protected void readJarFiles(String dirList) {
        ArrayList<JarFile> jarList = new ArrayList<JarFile>();

        StringTokenizer st = new StringTokenizer(dirList, File.pathSeparator);

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
    public List<Class<?>> getClassList(String attribName) {
        ArrayList<Class<?>> list = new ArrayList<Class<?>>();
        HashSet<String> classNames = new HashSet<String>();

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
                String names = ab.getValue(attribName);
                if (names != null) {
                    StringTokenizer st = new StringTokenizer(names, ", ");
                    while (st.hasMoreTokens()) {
                        String className = st.nextToken();
                        if (! classNames.contains(className)) {
                            try {
                                Class<?> cl = Class.forName(className, false, ld);
                                list.add(cl);
                                classNames.add(className);
                            } catch(Exception e) {
                                throw new Error("Could not load class " + className
                                        + ". Something wrong with jar "
                                        + jarFiles[i].getName() + "?", e);
                            }
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
     * @param clazz the class of which the returned classes are implementations
     *    or extensions.       
     * @return the list of classes.
     */
    public List<Class<?>> getClassList(String attribName, Class<?> clazz) {
        List<Class<?>> list = getClassList(attribName);

        for (Iterator<Class<?>> iter = list.iterator(); iter.hasNext();) {
            Class<?> cl = iter.next();
            if (! clazz.isAssignableFrom(cl)) {
                throw new Error("Class " + cl.getName()
                        + " cannot be assigned to class " + clazz.getName());
            }
        }
        return list;
    }
}
