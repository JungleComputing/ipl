/* $Id$ */

package ibis.io.rewriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

/**
 * The IOGenerator is responsible for collecting the classes to be rewritten and
 * rewriting them in the proper order.
 * 
 * @author Nick Palmer (npr200@few.vu.nl)
 */

public class IOGenerator extends ibis.compile.IbiscComponent implements RewriterConstants {

    boolean local = true;

    boolean file = false;

    boolean force_generated_calls = false;

    boolean silent = false;

    boolean use_jme = false;

    Vector<JavaClass> classes_to_rewrite, target_classes, classes_to_save;

    HashMap<String, JavaClass> arguments;

    boolean fromIbisc = false;

    public IOGenerator() {
        classes_to_rewrite = new Vector<JavaClass>();
        target_classes = new Vector<JavaClass>();
        classes_to_save = new Vector<JavaClass>();

        arguments = new HashMap<String, JavaClass>();

        silent = true;
    }

    protected boolean isVerbose() {
        return verbose;
    }

    protected boolean isFromIbisc() {
        return fromIbisc;
    }

    protected boolean forceGeneratedCalls() {
        return force_generated_calls;
    }

    protected boolean useJME() {
        return use_jme;
    }

    private boolean removeTarget(JavaClass clazz) {
        return target_classes.remove(clazz);
    }

    private void addTarget(JavaClass clazz) {
        target_classes.add(clazz);
    }

    private boolean removeSave(JavaClass clazz) {
        return classes_to_save.remove(clazz);
    }

    private void addSave(JavaClass clazz) {
        classes_to_save.add(clazz);
    }

    protected void replace(JavaClass clazz, JavaClass newclazz) {
        if (removeTarget(clazz)) {
            Repository.removeClass(clazz);
            Repository.addClass(newclazz);
            addTarget(newclazz);
        }
        if (removeSave(clazz)) {
            addSave(newclazz);
        }
    }

    protected void markRewritten(JavaClass clazz, JavaClass instgen) {
        if (isFromIbisc()) {
            setModified(wrapper.getInfo(clazz));
            if (instgen != null) {
        	addEntry(wrapper.getInfo(instgen), clazz.getClassName());
            }
        }
        addSave(clazz);
        if (instgen != null) {
            addSave(instgen);
        }
    }

    public IOGenerator(boolean verbose, boolean local, boolean file,
            boolean force_generated_calls, boolean silent, boolean use_jme) {
        this();
        this.verbose = verbose;
        this.local = local;
        this.file = file;
        this.force_generated_calls = force_generated_calls;
        this.silent = silent;
        this.use_jme = use_jme;
    }

    public boolean processArgs(ArrayList<String> args) {
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.equals("-iogen-force")) {
                force_generated_calls = true;
                args.remove(i);
                i--;
            }
            if (arg.equals("-jme")) {
                use_jme = true;
                args.remove(i);
                i--;
            }
        }
        return true;
    }

    public String getUsageString() {
        return "[-iogen-force] [-jme]";
    }

    public void process(Iterator<?> classes) {
        fromIbisc = true;
        arguments = new HashMap<String, JavaClass> ();
        for (Iterator<?> i = classes; i.hasNext();) {
            JavaClass cl = (JavaClass) i.next();
            arguments.put(cl.getClassName(), cl);
        }
        for (JavaClass cl : arguments.values()) {
            if (useJME()) {
                if (JMESerializationInfo.isJMESerializable(cl)) {
                    if (! JMESerializationInfo.isJMERewritten(cl)) {
                        addClass(cl);
                    }
                }
            }
            else {
                if (SerializationInfo.isSerializable(cl)) {
                    if (! SerializationInfo.isIbisSerializable(cl)) {
                        addClass(cl);
                    }
                }
            }
        }
        for (int i = 0; i < classes_to_rewrite.size(); i++) {
            JavaClass clazz = classes_to_rewrite.get(i);
            addReferencesToRewrite(clazz);
        }

        /* Sort class to rewrite. Super classes first.  */
        do_sort_classes(classes_to_rewrite);

        for (int i = 0; i < classes_to_rewrite.size(); i++) {
            JavaClass clazz = classes_to_rewrite.get(i);
            if (useJME()) {
                new JMECodeGenerator(this, clazz).generateEmptyMethods();
            }
            else {
                new CodeGenerator(this, clazz).generateEmptyMethods();
            }
        }

        if (verbose) {
            System.out.println("Ibisc: IOGenerator rewriting classes");
        }

        /* Sort target_classes. Super classes first.  */
        do_sort_classes(target_classes);

        for (int i = 0; i < target_classes.size(); i++) {
            JavaClass clazz = target_classes.get(i);
            if (verbose) {
                System.out.println("Target Class: " + clazz.getClassName());
            }
            if (!clazz.isInterface()) {
                if (!silent) {
                    System.out.println("  Rewrite class : "
                            + clazz.getClassName());
                }
                if (useJME()) {
                    new JMECodeGenerator(this, clazz).generateCode();
                }
                else {
                    new CodeGenerator(this, clazz).generateCode();
                }
            }
        }
    }

    public String rewriterImpl() {
        return "BCEL";
    }

    private void addTargetClass(JavaClass clazz) {
        if (useJME()) {
            if (verbose) {
                System.out.println("Considering target: " + clazz.getClassName());
            }
            if (!target_classes.contains(clazz)) {
                String nm = clazz.getClassName();
                if (arguments.containsKey(nm)) {
                    target_classes.add(clazz);
                    if (verbose) {
                        System.out.println("Adding jme target class : " + nm);
                    }
                }
            }			
        }
        else {
            if (!target_classes.contains(clazz) && !SerializationInfo.isIbisSerializable(clazz)) {
                String nm = clazz.getClassName();
                if (arguments.containsKey(nm)) {
                    target_classes.add(clazz);
                    if (verbose) {
                        System.out.println("Adding target class : " + nm);
                    }
                }
            }
        }
    }

    private void addRewriteClass(Type t, JavaClass clazz) {
        if (t instanceof ArrayType) {
            addRewriteClass(((ArrayType) t).getBasicType(), clazz);
        } else if (t instanceof ObjectType) {
            String name = ((ObjectType) t).getClassName();
            JavaClass c = CodeGenerator.lookupClass(name);
            if (c != null) {
                if (!local
                        || clazz.getPackageName().equals(c.getPackageName())) {
                    addClass(c);
                }
            }
        }
    }

    private void addRewriteClass(JavaClass clazz) {
        if (useJME()) {
            if (!classes_to_rewrite.contains(clazz) && JMESerializationInfo.isJMESerializable(clazz) && !JMESerializationInfo.isJMERewritten(clazz)) {
                classes_to_rewrite.add(clazz);
                if (verbose) {
                    System.out.println("Adding jme rewrite class : "
                            + clazz.getClassName());
                }
            }
        } else {
            if (!classes_to_rewrite.contains(clazz) && !SerializationInfo.isIbisSerializable(clazz)) {
                classes_to_rewrite.add(clazz);

                /*
                if (clazz.getMajor() > 50) {
                    clazz.setMajor(50);         // Dirty hack as long as we don't generate StackMap.
                }
                */

                if (verbose) {
                    System.out.println("Adding rewrite class : "
                            + clazz.getClassName());
                }
            }
        }
    }

    private void addClass(JavaClass clazz) {
        boolean serializable = false;

        if (!clazz.isClass()) {
            return;
        }

        if (clazz.getClassName().equals(TYPE_JAVA_LANG_CLASS)) {
            return;
        }

        if (clazz.getClassName().equals(TYPE_JAVA_LANG_STRING)) {
            return;
        }

        try {
            if (Repository.instanceOf(clazz, TYPE_JAVA_LANG_ENUM)) {
                return;
            }
        } catch(Exception e) {
            // Sigh: BCEL throws a NullPointerException if java.lang.Enum
            // does not exist
        }

        if (!classes_to_rewrite.contains(clazz)) {

            JavaClass super_classes[];
            
            try {
                super_classes = Repository.getSuperClasses(clazz);
            } catch(ClassNotFoundException e) {
                super_classes = null;
            }

            if (super_classes != null) {
                for (int i = 0; i < super_classes.length; i++) {
                    if (useJME()) {
                        if (JMESerializationInfo.isJMESerializable(super_classes[i])) {
                            serializable = true;
                            if (!JMESerializationInfo.isJMERewritten(super_classes[i])) {
                                if (!local
                                        || clazz.getPackageName().equals(
                                                super_classes[i].getPackageName())) {
                                    addRewriteClass(super_classes[i]);
                                }
                            } else {
                                if (verbose) {
                                    System.out.println(clazz.getClassName()
                                            + " already implements "
                                            + JMERewriterConstants.TYPE_IBIS_IO_JME_JMESERIALIZABLE);
                                }
                            }
                        }
                    }
                    else {
                        if (SerializationInfo.isSerializable(super_classes[i])) {
                            serializable = true;
                            if (!SerializationInfo.isIbisSerializable(super_classes[i])) {
                                if (!local
                                        || clazz.getPackageName().equals(
                                                super_classes[i].getPackageName())) {
                                    addRewriteClass(super_classes[i]);
                                }
                            } else {
                                if (verbose) {
                                    System.out.println(clazz.getClassName()
                                            + " already implements "
                                            + TYPE_IBIS_IO_SERIALIZABLE);
                                }
                            }
                        }
                    }
                }
            }
            if (useJME()) {
                serializable |= JMESerializationInfo.isJMESerializable(clazz);
            }
            else {
                serializable |= SerializationInfo.isSerializable(clazz);
            }
        } else {
            serializable = true;
        }

        if (serializable) {
            addRewriteClass(clazz);
            addTargetClass(clazz);
        }
    }

    void addReferencesToRewrite(JavaClass clazz) {

        /* Find all references to final reference types and add these to the
         * rewrite list
         */
        Field[] fields = clazz.getFields();

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];

            /* Don't send fields that are STATIC or TRANSIENT */
            if (!(field.isStatic() || field.isTransient())) {
                Type field_type = Type.getType(field.getSignature());

                if (!(field_type instanceof BasicType)
                        && (field_type != Type.STRING) && SerializationInfo.isFinal(field_type)) {
                    addRewriteClass(field_type, clazz);
                }
            }
        }
    }

    private void do_sort_classes(Vector<JavaClass> t) {
        int l = t.size();

        for (int i = 0; i < l; i++) {
            JavaClass clazz = t.get(i);
            int sav_index = i;
            for (int j = i + 1; j < l; j++) {
                JavaClass clazz2 = t.get(j);

                if (SerializationInfo.predecessor(clazz2.getClassName(), clazz)) {
                    clazz = clazz2;
                    sav_index = j;
                }
            }
            if (sav_index != i) {
                t.setElementAt(t.get(i), sav_index);
                t.setElementAt(clazz, i);
            }
        }
    }

    private void scanClass(Vector<String> classnames) {

        /* do the following here....

         for each of the classes in args

         - load it.
         - scan to see if it's parent is serializable
         - if so, add parent to rewrite list
         - scan to see if it is serializable
         - if so, add to rewrite list

         for each of the classes in the rewrite list

         - check if it contains references to final serializable objects
         - if so, add these objects to the rewrite list
         - check if it already extends ibis.io.Serializable
         - if not, add it and add the neccesary methods (empty)
         - check if it is a target
         - if so, add it to the target list

         for each of the objects on the target list

         - generate the code for the methods
         - save the class file

         */
        int lngth = classnames.size();
        Object[] names = classnames.toArray();

        java.util.Arrays.sort(names);

        for (int i = lngth - 1; i >= 0; i--) {
            String nm = (String) names[i];
            arguments.put(nm, null);
        }

        for (int i = lngth - 1; i >= 0; i--) {
            if (verbose) {
                System.out.println("  Loading class : " + (String) names[i]);
            }

            String className = (String) names[i];

            JavaClass clazz = null;
            if (!file) {
                clazz = CodeGenerator.lookupClass(className);
                if (clazz == null) {
                    System.err.println("Warning: could not load class "
                            + className + ". Please check your classpath.");
                }
            } else {

                System.err.println("class name = " + className);
                try {
                    ClassParser p = new ClassParser(className.replace('.',
                            java.io.File.separatorChar)
                            + ".class");
                    clazz = p.parse();
                    if (clazz != null) {
                        Repository.removeClass(className);
                        Repository.addClass(clazz);
                    }
                } catch (Exception e) {
                    System.err.println("got exception while loading class: "
                            + e);
                    System.exit(1);
                }
            }

            if (clazz != null) {
                if (SerializationInfo.isSerializable(clazz)) {
                    if (!SerializationInfo.isIbisSerializable(clazz)) {
                        addClass(clazz);
                    } else {
                        if (verbose) {
                            System.out.println(clazz.getClassName()
                                    + " already implements "
                                    + TYPE_IBIS_IO_SERIALIZABLE);
                        }
                    }
                } else {
                    if (verbose) {
                        System.out.println(clazz.getClassName()
                                + " is not serializable");
                    }
                    Repository.removeClass(clazz);
                }
            }
        }

        if (verbose) {
            System.out.println("Preparing classes");
        }

        for (int i = 0; i < classes_to_rewrite.size(); i++) {
            JavaClass clazz = classes_to_rewrite.get(i);
            addReferencesToRewrite(clazz);
        }

        Repository.clearCache();

        /* Sort class to rewrite. Super classes first.  */
        do_sort_classes(classes_to_rewrite);

        for (int i = 0; i < classes_to_rewrite.size(); i++) {
            JavaClass clazz = classes_to_rewrite.get(i);
            if (useJME()) {
                new JMECodeGenerator(this, clazz).generateEmptyMethods();
            }
            else {
                new CodeGenerator(this, clazz).generateEmptyMethods();
            }
        }

        if (verbose) {
            System.out.println("Rewriting classes");
        }

        /* Sort target_classes. Super classes first.  */
        do_sort_classes(target_classes);

        for (int i = 0; i < target_classes.size(); i++) {
            JavaClass clazz = target_classes.get(i);
            if (!clazz.isInterface()) {
                if (!silent) {
                    System.out.println("  Rewrite class : "
                            + clazz.getClassName());
                }
                if (useJME()) {
                    new JMECodeGenerator(this, clazz).generateCode();
                }
                else {
                    new CodeGenerator(this, clazz).generateCode();
                }
            }
        }

        if (verbose) {
            System.out.println("Saving classes");
        }

        for (int i = 0; i < classes_to_save.size(); i++) {
            JavaClass clazz = classes_to_save.get(i);
            String cl = clazz.getClassName();
            String classfile = "";

            try {
                if (local) {
                    int index = cl.lastIndexOf('.');
                    classfile = cl.substring(index + 1) + ".class";
                } else {
                    classfile = cl.replace('.', java.io.File.separatorChar)
                    + ".class";
                }
                if (verbose) {
                    System.out.println("  Saving class : " + classfile);
                }
                clazz.dump(classfile);
            } catch (IOException e) {
                System.err.println("got exception while writing " + classfile
                        + ": " + e);
                System.exit(1);
            }
        }
    }

    private static void usage() {
        System.out.println("Usage : java IOGenerator [-dir|-local] "
                + "[-package <package>] [-v] [-jme]"
                + "<fully qualified classname list | classfiles>");
        System.exit(1);
    }

    public static void main(String[] args) {
        boolean verbose = false;
        boolean local = true;
        boolean file = false;
        boolean force_generated_calls = false;
        boolean silent = false;
        boolean use_jme = false;
        Vector<String> files = new Vector<String>();
        String pack = null;

        if (args.length == 0) {
            usage();
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-v")) {
                verbose = true;
            } else if (!args[i].startsWith("-")) {
                files.add(args[i]);
            } else if (args[i].equals("-dir")) {
                local = false;
            } else if (args[i].equals("-local")) {
                local = true;
            } else if (args[i].equals("-file")) {
                file = true;
            } else if (args[i].equals("-silent")) {
                silent = true;
            } else if (args[i].equals("-force")) {
                force_generated_calls = true;
            } else if (args[i].equals("-jme")) {
                use_jme = true;
            } else if (args[i].equals("-package")) {
                pack = args[i + 1];
                i++; // skip arg
            } else {
                usage();
            }
        }

        Vector<String> newArgs = new Vector<String>();
        for (int i = 0; i < files.size(); i++) {
            String name = files.elementAt(i);

            int colon = name.indexOf(':');
            if (colon != -1) {
                name = name.substring(colon + 1);
            }

            int index = name.lastIndexOf(".class");

            if (index != -1) {
                name = name.substring(0, index);
                name = name.replace(java.io.File.separatorChar, '.');
                if (pack == null) {
                    newArgs.add(name);
                } else {
                    newArgs.add(pack + "." + name);
                }
            } else {
                File f = new File(name);

                name = name.replace(java.io.File.separatorChar, '.');

                if (f.isDirectory()) {
                    processDirectory(f, newArgs, name);
                    continue;
                }
                if (pack == null) {
                    newArgs.add(name);
                } else {
                    newArgs.add(pack + "." + name);
                }
            }
        }

        new IOGenerator(verbose, local, file, force_generated_calls, 
                silent, use_jme).scanClass(newArgs);
    }

    private static void processDirectory(File f, Vector<String> args,
            String name) {
        File[] list = f.listFiles();
        String prefix = "";

        if (!name.equals(".")) {
            prefix = name + ".";
        }
        for (int i = 0; i < list.length; i++) {
            String fname = list[i].getName();
            if (list[i].isDirectory()) {
                processDirectory(list[i], args, prefix + fname);
            } else {
                int index = fname.lastIndexOf(".class");
                if (index != -1) {
                    args.add(prefix + fname.substring(0, index));
                }
            }
        }
    }
}
