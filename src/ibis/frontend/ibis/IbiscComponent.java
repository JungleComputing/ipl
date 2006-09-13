/* $Id:$ */

package ibis.frontend.ibis;

import ibis.frontend.generic.RunJavac;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This abstract class must be implemented by all components that are to be part
 * of the Ibis frontend framework, and is the only means through which the
 * Ibis frontend framework communicates with the component.
 */
public abstract class IbiscComponent {

    /** Verbose flag. */
    protected boolean verbose;

    /** Keep generated Java files around. */
    protected boolean keep;

    /** Set when instantiated from Ibisc. */
    protected boolean fromIbisc = false;

    /** Wrapper for specific bytecode rewriter implementation. */
    protected ByteCodeWrapper wrapper;

    private HashMap allClasses;

    private HashMap newClasses = new HashMap();

    private class ClassIterator implements Iterator {
        Iterator i;
        public ClassIterator() {
            i = allClasses.values().iterator();
        }

        public boolean hasNext() {
            return i.hasNext();
        }

        public Object next() {
            IbiscEntry e = (IbiscEntry) i.next();
            return e.cl.getClassObject();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    void setWrapper(ByteCodeWrapper w) {
        wrapper = w;
    }

    /**
     * This method accepts an array of program arguments, processes the
     * arguments that are intended for this component, and removes these
     * arguments from the list. The parameter array may/must be modified.
     * @param args the array of program arguments.
     * @return <code>true</code> when this component must be run,
     * <code>false</code> otherwise.
     * @exception IllegalArgumentException may be thrown when there was an error
     *    in the arguments.
     */
    public abstract boolean processArgs(ArrayList args);

    /**
     * This method processes an iterator which delivers java classes.
     * The values are classes as represented by the byte code rewriter used.
     * The component processes these entries as it sees fit.
     * It can add entries by means of the  {@link #addEntry()} method.
     * @param classes the class iterator to process.
     */
    public abstract void process(Iterator classes);

    /**
     * Should return the rewriter implementation, for now only BCEL is
     * supported. In the future, ASM may be supported as well.
     * @return the rewriter implementation needed/used by this
     * <code>IbiscComponent</code>.
     */
    public abstract String rewriterImpl();

    void processClasses(HashMap classes) {
        allClasses = classes;
        process(new ClassIterator());
        for (Iterator i = newClasses.values().iterator(); i.hasNext();) {
            IbiscEntry ie = (IbiscEntry) i.next();
            allClasses.put(ie.cl.getClassName(), ie);
        }
    }

    /**
     * Returns the name of the directory in which the file was found from
     * which the class indicated by the specified class name was read.
     * Returns <code>null</code> if the class is not found or the file lived
     * in the current directory.
     * @param cl the classname.
     * @return the directory name or <code>null</code>.
     */
    protected String getDirectory(String cl) {
        IbiscEntry ie = (IbiscEntry) allClasses.get(cl);
        if (ie == null) {
            ie = (IbiscEntry) newClasses.get(cl);
        }
        if (ie == null) {
            return null;
        }
        File f;
        if (ie.jarInfo == null) {
            f = new File(ie.fileName);
        } else {
            f = new File(ie.jarInfo.jarFile.getName());
        }
        return f.getParent();
    }

    /**
     * Set the verbose flag to the specified value.
     * @param v the value.
     */
    void setVerbose(boolean v) {
        verbose = v;
    }

    /**
     * Set the keep flag to the specified value.
     * When set, this flag instructs the component not to remove Java
     * files that it may generate while processing the classes.
     * @param v the value.
     */
    void setKeep(boolean v) {
        keep = v;
    }

    /**
     * Writes out all modified classes and jars.
     */
    protected void writeAll() {
        Ibisc.writeAll();
    }

    /**
     * Notifies that the specified class has changed.
     * @param cl the class that has changed.
     */
    protected void setModified(ClassInfo cl) {
        String className = cl.getClassName();
        IbiscEntry e = (IbiscEntry) allClasses.get(className);
        if (e == null) {
            e = (IbiscEntry) newClasses.get(className);
        }
        if (e != null) {
            e.cl = cl;
            e.modified = true;
        }
    }

    /**
     * Adds a new entry to the class list. The new entry is derived from
     * the specified class, and is ultimately written in the same directory,
     * or the same jar file as the class from which it is derived.
     * @param cl the new entry.
     * @param fromClass the name of the class from which it is derived.
     */
    protected void addEntry(ClassInfo cl, String fromClass) {
        IbiscEntry from = (IbiscEntry) allClasses.get(fromClass);
        String fn = from.fileName;
        String className = cl.getClassName();
        String baseDir = (new File(fn)).getParent();
        String newFilename;
        int n = className.lastIndexOf(".");
        String name = className;
        if (n != -1) {
            name = name.substring(n+1, name.length());
        }
        if (baseDir == null) {
            newFilename = name + ".class";
        } else {
            newFilename = baseDir + File.separator + name + ".class";
        }
        IbiscEntry newEntry = new IbiscEntry(cl, newFilename);
        newEntry.modified = true;
        newEntry.jarInfo = from.jarInfo;
        newClasses.put(className, newEntry);
        if (from.jarInfo != null) {
            from.jarInfo.addEntry(newEntry);
        }
    }

    /**
     * Compiles the specified list of arguments, which are all derived from
     * the specified class. The resulting classes are ultimately written in
     * the same directory or the same jar file as the class from which they
     * are derived.
     * @param args the list of Java files to compile.
     * @param fromClass the name of the class from which they are derived.
     */
    protected void compile(ArrayList args, String fromClass) {
        int sz = args.size();
        String[] compilerArgs = new String[sz + 1];
        compilerArgs[0] = "-g";
        for (int i = 0; i < sz; i++) {
            compilerArgs[i + 1] = (String) args.get(i);
        }
        if (!RunJavac.runJavac(compilerArgs, verbose)) {
            System.exit(1);
        }

        for (int i = 1; i <= sz; i++) {
            if (!keep) { // remove generated files 
                File f = new File(compilerArgs[i]);
                f.delete();
            }
        }

        // When called from Ibisc, parse resulting class files and remove
        // them.
        if (fromIbisc) {
            for (int i = 1; i <= sz; i++) {
                File f = new File(compilerArgs[i]);
                File base = f.getParentFile();
                if (base == null) {
                    base = new File(".");
                }
                String fn = f.getName();
                fn = fn.substring(0, fn.length()-5);
                String[] list = base.list();
                for (int j = 0; j < list.length; j++) {
                    if (list[j].startsWith(fn) && list[j].endsWith(".class")) {
                        String name = base.getPath() + File.separator + list[j];
                        try {
                            ClassInfo cl = wrapper.parseClassFile(name);
                            addEntry(cl, fromClass);
                            (new File(name)).delete();
                        } catch(Exception e) {
                            System.err.println("Could not read " + name);
                            System.exit(1);
                        }
                    }
                }
            }
        }
    }
}
