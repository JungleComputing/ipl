/* $Id$ */

package ibis.frontend.ibis;

import ibis.util.ClassLister;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

/**
 * The main class of the Ibisc frontend.
 * Loads all Ibisc components that are found on the classpath, and runs them
 * all on the specified arguments.
 */
public class Ibisc {

    /** Contains the (classname, <code>IbiscEntry</code>) pairs. */
    public static HashMap allClasses = new HashMap();

    /** The list of jarfiles. */
    private static ArrayList jarFiles = new ArrayList();

    /** Verbose flag. */
    private static boolean verbose = false;

    /** Set when jar files must be compressed. */
    private static boolean compress = true;

    private static ByteCodeWrapper w;

    private static void getClassesFromDirectory(File f, String prefix) {
        File[] list = f.listFiles();

        if (! prefix.equals("")) {
            prefix = prefix + f.getName() + File.separator;
        } else {
            prefix = f.getName() + File.separator;
        }

        for (int i = 0; i < list.length; i++) {
            String fname = list[i].getName();
            if (list[i].isDirectory()) {
                getClassesFromDirectory(list[i], prefix);
            } else if (fname.endsWith(".class")) {
                getClassFromClassFile(prefix + fname);
            } else if (fname.endsWith(".jar")) {
                getClassesFromJarFile(list[i], prefix + fname);
            }
        }
    }

    private static void getClassFromClassFile(String fileName) {
        ClassInfo cl = null;
        try {
            cl = w.parseClassFile(fileName);
        } catch(IOException e) {
            System.err.println("Ibisc: warning: could not read class "
                    + fileName);
        }
        if (cl != null) {
            allClasses.put(cl.getClassName(), new IbiscEntry(cl, fileName));
        }
    }

    private static void getClassesFromJarFile(File f, String fileName) {
        JarFile jf = null;
        JarInfo jarInfo = null;
        try {
            jf = new JarFile(f, true);
            jarInfo = new JarInfo(jf);
        } catch(IOException e) {
            System.err.println("Ibisc: warning: could not read jarfile "
                    + fileName);
            return;
        }
        jarFiles.add(jarInfo);
        // jarInfo.modified = true;        // for testing ...
        for (Enumeration iitems = jarInfo.entries(); iitems.hasMoreElements();) {
            JarEntryInfo ient = (JarEntryInfo) iitems.nextElement();
            String iname = ient.jarEntry.getName();
            if (iname.endsWith(".class")) {
                try {
                    ClassInfo cl = w.parseInputStream(ient.getInputStream(),
                                iname);
                    IbiscEntry entry = new IbiscEntry(cl, iname);
                    allClasses.put(cl.getClassName(), entry);
                    entry.jarInfo = jarInfo;
                    ient.ibiscEntry = entry;
                } catch(IOException e) {
                    System.err.println("Ibisc: warning: could not read "
                            + "class " + iname + " from jar file "
                            + fileName);
                }
            }
        }
    }

    /**
     * Verifies all modified classes.
     * @param ic the <code>IbiscComponent</code> after which this
     * verification is run.
     */
    private static void verifyClasses(IbiscComponent ic) {
        for (Iterator i = allClasses.values().iterator(); i.hasNext();) {
            IbiscEntry e = (IbiscEntry) i.next();
            if (e.modified) {
                if (! e.cl.doVerify()) {
                    System.out.println("Ibisc: verification failed after "
                            + "component " + ic.getClass().getName());
                    System.exit(1);
                }
            }
        }
    }

    /**
     * Writes all modified classes that are not part of a jar.
     */
    private static void writeClasses() {
        for (Iterator i = allClasses.values().iterator(); i.hasNext();) {
            IbiscEntry e = (IbiscEntry) i.next();
            if (e.modified && e.jarInfo == null) {
                File temp = null;
                try {
                    File canonicalDir = new File(e.fileName).getCanonicalFile().getParentFile();
                    temp = File.createTempFile("Ibisc_", null, canonicalDir);
                    e.cl.dump(temp.getCanonicalPath());
                    if (! temp.renameTo(new File(e.fileName))) {
                        throw new Exception("Could not write " + e.fileName);
                    }
                } catch (Exception ex) {
                    System.err.println("Ibisc: got exception while writing "
                            + e.fileName + ": " + ex);
                    if (temp != null) {
                        temp.delete();
                    }
                    System.exit(1);
                }
                e.modified = false;
            }
        }
    }

    /**
     * Writes all jars that have a modified (or added) entry.
     */
    private static void writeJars() {
        // First, determine which jars have actually changed.
        for (Iterator i = allClasses.values().iterator(); i.hasNext();) {
            IbiscEntry e = (IbiscEntry) i.next();
            if (e.modified && e.jarInfo != null) {
                e.jarInfo.modified = true;
            }
        }

        // Then, write ...
        for (int i = 0; i < jarFiles.size(); i++) {
            JarInfo ji = (JarInfo) jarFiles.get(i);
            if (ji.modified) {
                String name = ji.jarFile.getName();
                File temp = null;
                try {
                    File canonicalDir = new File(name).getCanonicalFile().getParentFile();
                    temp = File.createTempFile("Ibisc_", null, canonicalDir);
                    FileOutputStream out = new FileOutputStream(temp);
                    BufferedOutputStream bo = new BufferedOutputStream(out, 16384);
                    ZipOutputStream zo = new ZipOutputStream(bo);
                    zo.setMethod(ZipOutputStream.DEFLATED);
                    if (! compress) {
                        zo.setLevel(0);
                    }
                    for (Enumeration iitems = ji.entries(); iitems.hasMoreElements();) {
                        JarEntryInfo ient = (JarEntryInfo) iitems.nextElement();
                        ient.write(zo);
                    }
                    zo.close();
                    if (! temp.renameTo(new File(name))) {
                        throw new Exception("Could not write " + name);
                    }
                } catch(Exception e) {
                    System.err.println("Ibisc: got exception while writing "
                            + name + ": " + e);
                    e.printStackTrace();
                    if (temp != null) {
                        temp.delete();
                    }
                    System.exit(1);
                }
                ji.modified = false;
            }
        }
    }

    /**
     * Writes all classes and jars that have been modified.
     */
    static void writeAll() {
        writeClasses();
        writeJars();
    }

    /**
     * Reads all classes and jars from the specified arguments.
     * @param leftArgs the arguments.
     */
    static void readAll(ArrayList leftArgs) {
        // Convert the rest of the arguments to classes
        for (int i = 0; i < leftArgs.size(); i++) {
            String arg = (String) leftArgs.get(i);
            File f = new File(arg);
            if (f.isDirectory()) {
                getClassesFromDirectory(f, "");
            } else if (arg.endsWith(".class")) {
                getClassFromClassFile(arg);
            } else if (arg.endsWith(".jar")) {
                getClassesFromJarFile(f, arg);
            } else {
                System.err.println("Ibisc: illegal argument: " + arg
                        + " is not a jar or class file.");
                System.exit(1);

            }
        }
    }

    public static void main(String[] args) {
        boolean keep = false;
        boolean verify = false;
        ArrayList leftArgs = new ArrayList();

        // Process own arguments.
        for (int i = 0; i < args.length; i++) {
            if (false) {
            } else if (args[i].equals("-v") || args[i].equals("-verbose")) {
                verbose = true;
            } else if (args[i].equals("-no-verbose")) {
                verbose = false;
            } else if (args[i].equals("-verify")) {
                verify = true;
            } else if (args[i].equals("-no-verify")) {
                verify = false;
            } else if (args[i].equals("-keep")) {
                keep = true;
            } else if (args[i].equals("-no-keep")) {
                keep = false;
            } else if (args[i].equals("-compress")) {
                compress = true;
            } else if (args[i].equals("-no-compress")) {
                compress = false;
            } else {
                leftArgs.add(args[i]);
            }
        }

        // Obtain a list of Ibisc components.
        ClassLister clstr = ClassLister.getClassLister(null);
        List clcomponents = clstr.getClassList("Ibisc-Component", IbiscComponent.class);
        ArrayList components = new ArrayList();

        // If no classes found, at least add IOGenerator.
        if (clcomponents.size() == 0) {
            try {
                Class cl = Class.forName("ibis.frontend.io.IOGenerator");
                clcomponents.add(cl);
            } catch(Exception e) {
                // Ignore
            }
        }

        // Instantiate Ibisc components.
        for (Iterator iter = clcomponents.iterator(); iter.hasNext();) {
            Class cl = (Class) iter.next();
            IbiscComponent ic = null;
            try {
                ic = (IbiscComponent) cl.newInstance();
                ic.setVerbose(verbose);
                ic.fromIbisc = true;
                ic.setKeep(keep);
                if (ic.processArgs(leftArgs)) {
                    components.add(ic);
                }
            } catch(Exception e) {
                System.err.println("Ibisc: warning: could not instantiate "
                        + cl.getName());
            }
        }

        // Check for unrecognized arguments.
        for (int i = 0; i < leftArgs.size(); i++) {
            String arg = (String) leftArgs.get(i);
            if (arg.startsWith("-")) {
                System.err.println("Ibisc: unrecognized argument: " + arg);
                System.exit(1);
            }
        }

        // IOGenerator should be last
        int szm1 = components.size() - 1;
        for (int i = 0; i < szm1; i++) {
            IbiscComponent ic = (IbiscComponent) components.get(i);
            if (ic instanceof ibis.frontend.io.IOGenerator) {
                components.set(i, components.get(szm1));
                components.set(szm1, ic);
                break;
            }
        }

        if (components.size() == 0) {
            System.err.println("Ibisc: warning: no components found!");
        }

        String wrapperKind = null;
        // Make all components process all classes.
        for (int i = 0; i < components.size(); i++) {
            IbiscComponent ic = (IbiscComponent) components.get(i);
            String knd = ic.rewriterImpl();
            if (wrapperKind == null || ! knd.equals(wrapperKind)) {
                if (wrapperKind != null) {
                    writeAll();
                }
                allClasses.clear();
                wrapperKind = knd;
                if (knd.equals("BCEL")) {
                    w = new BCELWrapper();
                } else {
                    System.err.println("Ibisc: component "
                            + ic.getClass().getName()
                            + ": unsupported bytecode rewriter: " + knd);
                }
                readAll(leftArgs);
            }
            ic.setWrapper(w);
            ic.processClasses(allClasses);
            if (verify) {
                // Verify after each component.
                verifyClasses(ic);
            }
        }

        // Now, write out all modified stuff.
        writeAll();
    }
}
