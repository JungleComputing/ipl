/* $Id$ */

package ibis.compile;

import ibis.util.ClassLister;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

/**
 * The main class of the Ibisc compiler.
 * Loads all Ibisc components that are found on the classpath, and runs them
 * all on the specified arguments.
 */
public class Ibisc {

    /** Contains the (classname, <code>IbiscEntry</code>) pairs. */
    public static HashMap<String, IbiscEntry> allClasses
            = new HashMap<String, IbiscEntry>();

    /** The list of jarfiles. */
    private static ArrayList<JarInfo> jarFiles = new ArrayList<JarInfo>();

    /** Verbose flag. */
    private static boolean verbose = false;

    /** Set when jar files must be compressed. */
    private static boolean compress = true;

    static ByteCodeWrapper w;

    static ByteCodeWrapper bcelWrapper;

    private static ArrayList<IbiscComponent> ibiscComponents
            = new ArrayList<IbiscComponent>();

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
    }

    /**
     * Verifies all modified classes.
     * @param ic the <code>IbiscComponent</code> after which this
     * verification is run.
     */
    private static void verifyClasses(IbiscComponent ic) {
        for (IbiscEntry e : allClasses.values()) {
            if (e.getModified()) {
                if (! e.getClassInfo().doVerify()) {
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
        for (IbiscEntry e : allClasses.values()) {
            if (e.getModified() && e.getJarInfo() == null) {
                File temp = null;
                try {
                    File file = new File(e.fileName);
                    File canonicalDir = file.getCanonicalFile().getParentFile();
                    temp = File.createTempFile("Ibisc_", null, canonicalDir);
                    e.getClassInfo().dump(temp.getCanonicalPath());
                    rename(file, temp, canonicalDir);
                } catch (Exception ex) {
                    System.err.println("Ibisc: got exception while writing "
                            + e.fileName + ": " + ex);
                    if (temp != null) {
                        temp.delete();
                    }
                    System.exit(1);
                }
                e.setModified(false);
            }
        }
    }

    /**
     * Safe(?) rename. Problem is that File.rename may not work if the
     * destination exists.
     */
    private static void rename(File dest, File src, File dir) throws IOException {
        File temp = File.createTempFile("Ibc_", null, dir);
        temp.delete();
        if (dest.exists() && ! dest.renameTo(temp)) {
            throw new IOException("Could not rename "
                    + dest.getName() + " to temporary");
        }
        if (! src.renameTo(dest)) {
            if (temp.exists()) {
                temp.renameTo(dest);    // Try to restore dest. If it fails,
                                        // we at least have temp.
            }
            throw new IOException("Could not rename to "
                    + dest.getName());
        }
        temp.delete();
    }

    /**
     * Writes all jars that have a modified (or added) entry.
     */
    private static void writeJars() {
        // First, determine which jars have actually changed.
        for (IbiscEntry e : allClasses.values()) {
            if (e.getModified()) {
                JarInfo j = e.getJarInfo();
                if (j != null) {
                    j.setModified(true);
                }
            }
        }

        // Then, write ...
        for (int i = 0; i < jarFiles.size(); i++) {
            JarInfo ji = jarFiles.get(i);
            if (ji.getModified()) {
                String name = ji.getName();
                File temp = null;
                try {
                    File file = new File(name);
                    File canonicalDir = file.getCanonicalFile().getParentFile();
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
                    rename(file, temp, canonicalDir);
                } catch(Exception e) {
                    System.err.println("Ibisc: got exception while writing "
                            + name + ": " + e);
                    e.printStackTrace();
                    if (temp != null) {
                        temp.delete();
                    }
                    System.exit(1);
                }
                ji.setModified(false);
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
    static void readAll(ArrayList<String> leftArgs) {
        // Convert the rest of the arguments to classes
        for (String arg : leftArgs) {
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

    private static String usage() {
        String rval = "Usage: java ibis.compile.Ibisc [-verbose] [-verify] [-keep] [-help] ";
        for (int i = 0; i < ibiscComponents.size(); i++) {
            IbiscComponent ic = ibiscComponents.get(i);
            String s = ic.getUsageString();
            if (! s.equals("")) {
                rval = rval + s + " ";
            }
        }
        return rval + " <jar-file|dir|class-file>+";
    }

    /**
     * Main entry point for Ibisc
     * @param args arguments from the command line
     */
    public static void main(String[] args) {
        boolean keep = false;
        boolean verify = false;
        boolean help = false;
        ArrayList<String> leftArgs = new ArrayList<String>();

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
            } else if (args[i].equalsIgnoreCase("--help")
                    || args[i].equalsIgnoreCase("-h")
                    || args[i].equalsIgnoreCase("-help")
                    || args[i].equalsIgnoreCase("/?")) {
                help = true;
            } else {
                leftArgs.add(args[i]);
            }
        }

        bcelWrapper = new BCELWrapper(leftArgs);

        // Obtain a list of Ibisc components.
        ClassLister clstr = ClassLister.getClassLister(null);
        List<Class> clcomponents = clstr.getClassList("Ibisc-Component", IbiscComponent.class);
        ArrayList<IbiscComponent> components = new ArrayList<IbiscComponent>();

        // Instantiate Ibisc components.
        for (Class cl : clcomponents) {
            IbiscComponent ic = null;
            try {
                ic = (IbiscComponent) cl.newInstance();
                ic.setVerbose(verbose);
                ic.fromIbisc = true;
                ic.setKeep(keep);
                if (ic.processArgs(leftArgs)) {
                    components.add(ic);
                }
                ibiscComponents.add(ic);
            } catch(Exception e) {
                System.err.println("Ibisc: warning: could not instantiate "
                        + cl.getName());
            }
        }

        // Check for unrecognized arguments.
        for (String arg : leftArgs) {
            if (arg.startsWith("-")) {
                System.err.println("Ibisc: unrecognized argument: " + arg);
                System.err.println(usage());
                System.exit(1);
            }
        }

        if (help) {
            System.out.println(usage());
            System.exit(0);
        }

        if (leftArgs.size() == 0) {
            System.err.println("Ibisc: no files to process?");
            System.err.println(usage());
            System.exit(1);
        }

        // IOGenerator should be last
        int szm1 = components.size() - 1;
        for (int i = 0; i < szm1; i++) {
            IbiscComponent ic = components.get(i);
            if (ic.getClass().getName().equals("ibis.io.rewriter.IOGenerator")) {
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
            IbiscComponent ic = components.get(i);
            String knd = ic.rewriterImpl();
            if (wrapperKind == null || ! knd.equals(wrapperKind)) {
                if (wrapperKind != null) {
                    writeAll();
                }
                allClasses.clear();
                wrapperKind = knd;
                if (knd.equals("BCEL")) {
                    w = bcelWrapper;
                } else {
                    System.err.println("Ibisc: component "
                            + ic.getClass().getName()
                            + ": unsupported bytecode rewriter: " + knd);
                }
                readAll(leftArgs);
            }
            ic.setWrapper(w);
            System.out.println("Ibisc: applying rewriter "
                    + ic.getClass().getName()
                    + " to " + allClasses.size() + " classes.");
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
