/* $Id: IOGenerator.java 14636 2012-04-12 18:01:26Z ceriel $ */

package ibis.io.rewriter;

import ibis.compile.ASMRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * The ASMIOGenerator is responsible for collecting the classes to be rewritten and
 * rewriting them in the proper order.
 */

public class ASMIOGenerator extends ibis.compile.IbiscComponent implements ASMRewriterConstants, Opcodes {

    boolean local = true;

    boolean file = false;

    boolean force_generated_calls = false;

    boolean silent = false;

    boolean use_jme = false;

    Vector<ClassNode> classes_to_rewrite, target_classes, classes_to_save;

    HashMap<String, ClassNode> arguments;

    boolean fromIbisc = false;

    public ASMIOGenerator() {
        classes_to_rewrite = new Vector<ClassNode>();
        target_classes = new Vector<ClassNode>();
        classes_to_save = new Vector<ClassNode>();

        arguments = new HashMap<String, ClassNode>();

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

    private boolean removeTarget(ClassNode clazz) {
        return target_classes.remove(clazz);
    }

    private void addTarget(ClassNode clazz) {
        target_classes.add(clazz);
    }

    private boolean removeSave(ClassNode clazz) {
        return classes_to_save.remove(clazz);
    }

    private void addSave(ClassNode clazz) {
        classes_to_save.add(clazz);
    }

    protected void replace(ClassNode clazz, ClassNode newclazz) {
        if (removeTarget(clazz)) {
            ASMRepository.removeClass(clazz.name);
            ASMRepository.addClass(newclazz);
            addTarget(newclazz);
        }
        if (removeSave(clazz)) {
            addSave(newclazz);
        }
    }

    protected void markRewritten(ClassNode clazz, ClassNode instgen) {
        if (isFromIbisc()) {
            setModified(wrapper.getInfo(clazz));
            if (instgen != null) {
        	addEntry(wrapper.getInfo(instgen), clazz.name.replaceAll("/", "."));
            }
        }
        addSave(clazz);
        if (instgen != null) {
            addSave(instgen);
        }
    }

    public ASMIOGenerator(boolean verbose, boolean local, boolean file,
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
        arguments = new HashMap<String, ClassNode> ();
        for (Iterator<?> i = classes; i.hasNext();) {
            ClassNode cl = (ClassNode) i.next();
            arguments.put(cl.name, cl);
        }
        for (ClassNode cl : arguments.values()) {
            if (useJME()) {
                /* TODO
                if (JMESerializationInfo.isJMESerializable(cl)) {
                    if (! JMESerializationInfo.isJMERewritten(cl)) {
                        addClass(cl);
                    }
                }
                */
            }
            else {
                if (ASMSerializationInfo.isSerializable(cl)) {
                    if (! ASMSerializationInfo.isIbisSerializable(cl)) {
                        addClass(cl);
                    }
                }
            }
        }
        for (int i = 0; i < classes_to_rewrite.size(); i++) {
            ClassNode clazz = classes_to_rewrite.get(i);
            addReferencesToRewrite(clazz);
        }

        /* Sort class to rewrite. Super classes first.  */
        do_sort_classes(classes_to_rewrite);

        for (int i = 0; i < classes_to_rewrite.size(); i++) {
            ClassNode clazz = classes_to_rewrite.get(i);
            if (useJME()) {
                // TODO new JMECodeGenerator(this, clazz).generateEmptyMethods();
            }
            else {
                new ASMCodeGenerator(this, clazz).generateEmptyMethods();
            }
        }

        if (verbose) {
            System.out.println("Ibisc: IOGenerator rewriting classes");
        }

        /* Sort target_classes. Super classes first.  */
        do_sort_classes(target_classes);

        for (int i = 0; i < target_classes.size(); i++) {
            ClassNode clazz = target_classes.get(i);
            if (verbose) {
                System.out.println("Target Class: " + clazz.name);
            }
            if ((clazz.access & ACC_INTERFACE) != ACC_INTERFACE) {
                if (!silent) {
                    System.out.println("  Rewrite class : "
                            + clazz.name);
                }
                if (useJME()) {
                    // TODO new JMECodeGenerator(this, clazz).generateCode();
                }
                else {
                    new ASMCodeGenerator(this, clazz).generateCode();
                }
            }
        }
    }

    public String rewriterImpl() {
        return "ASM";
    }

    private void addTargetClass(ClassNode clazz) {
        if (useJME()) {
            /* TODO
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
            */			
        }
        else {
            if (!target_classes.contains(clazz) && !ASMSerializationInfo.isIbisSerializable(clazz)) {
                String nm = clazz.name;
                if (arguments.containsKey(nm)) {
                    target_classes.add(clazz);
                    if (verbose) {
                        System.out.println("Adding target class : " + nm);
                    }
                }
            }
        }
    }

    private void addRewriteClass(Type t, ClassNode clazz) {
        if (t.getSort() == Type.ARRAY) {
            addRewriteClass(t.getElementType(), clazz);
        } else if (t.getSort() == Type.OBJECT) {
            String name = t.getInternalName();
            ClassNode c = ASMCodeGenerator.lookupClass(name);
            if (c != null) {
                if (!local
                        || ASMCodeGenerator.getPackageName(clazz.name).equals(ASMCodeGenerator.getPackageName(c.name))) {
                    addClass(c);
                }
            }
        }
    }

    private void addRewriteClass(ClassNode clazz) {
        if (useJME()) {
            /* TODO
            if (!classes_to_rewrite.contains(clazz) && JMESerializationInfo.isJMESerializable(clazz) && !JMESerializationInfo.isJMERewritten(clazz)) {
                classes_to_rewrite.add(clazz);
                if (verbose) {
                    System.out.println("Adding jme rewrite class : "
                            + clazz.getClassName());
                }
            }
            */
        } else {
            if (!classes_to_rewrite.contains(clazz) && !ASMSerializationInfo.isIbisSerializable(clazz)) {
                classes_to_rewrite.add(clazz);
                if (verbose) {
                    System.out.println("Adding rewrite class : "
                            + clazz.name);
                }
            }
        }
    }

    private void addClass(ClassNode clazz) {
        boolean serializable = false;

        if (clazz.name.equals(JAVA_LANG_CLASS)) {
            return;
        }

        if (clazz.name.equals(JAVA_LANG_STRING)) {
            return;
        }

        try {
            if (ASMRepository.instanceOf(clazz, JAVA_LANG_ENUM)) {
                return;
            }
        } catch (ClassNotFoundException e1) {
            // ignore
        }

        if (!classes_to_rewrite.contains(clazz)) {

            ClassNode[] super_classes;
            
            try {
                super_classes = ASMRepository.getSuperClasses(clazz);
            } catch(ClassNotFoundException e) {
                super_classes = null;
            }

            if (super_classes != null) {
                for (int i = 0; i < super_classes.length; i++) {
                    if (useJME()) {
                        /* TODO
                        if (JMESerializationInfo.isJMESerializable(super_classes[i])) {
                            serializable = true;
                            if (!JMESerializationInfo.isJMERewritten(super_classes[i])) {
                                if (!local
                                        || clazz.getPa ckageName().equals(
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
                        */
                    }
                    else {
                        if (ASMSerializationInfo.isSerializable(super_classes[i])) {
                            serializable = true;
                            if (!ASMSerializationInfo.isIbisSerializable(super_classes[i])) {
                                if (!local
                                        || ASMCodeGenerator.getPackageName(clazz.name).equals(
                                                ASMCodeGenerator.getPackageName(super_classes[i].name))) {
                                    addRewriteClass(super_classes[i]);
                                }
                            } else {
                                if (verbose) {
                                    System.out.println(clazz.name
                                            + " already implements "
                                            + IBIS_IO_SERIALIZABLE);
                                }
                            }
                        }
                    }
                }
            }
            if (useJME()) {
                /* TODO
                serializable |= JMESerializationInfo.isJMESerializable(clazz);
                */
            }
            else {
                serializable |= ASMSerializationInfo.isSerializable(clazz);
            }
        } else {
            serializable = true;
        }

        if (serializable) {
            addRewriteClass(clazz);
            addTargetClass(clazz);
        }
    }

    void addReferencesToRewrite(ClassNode clazz) {

        /* Find all references to final reference types and add these to the
         * rewrite list
         */
        @SuppressWarnings("unchecked")
        List<FieldNode> fields = clazz.fields;

        for (FieldNode field : fields) {
            /* Don't send fields that are STATIC or TRANSIENT */
            if ((field.access & (ACC_STATIC | ACC_TRANSIENT)) == 0) {
                Type field_type = Type.getType(field.desc);
                while (field_type.getSort() == Type.ARRAY) {
                    field_type = field_type.getElementType();
                }
                if (field_type.getSort() == Type.OBJECT
                        && ! field_type.getInternalName().equals(JAVA_LANG_STRING) 
                        && ASMSerializationInfo.isFinal(field_type)) {
                    addRewriteClass(field_type, clazz);
                }
            }
        }
    }

    private void do_sort_classes(Vector<ClassNode> t) {
        int l = t.size();

        for (int i = 0; i < l; i++) {
            ClassNode clazz = t.get(i);
            int sav_index = i;
            for (int j = i + 1; j < l; j++) {
                ClassNode clazz2 = t.get(j);

                if (ASMSerializationInfo.predecessor(clazz2.name, clazz)) {
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

            ClassNode clazz = null;
            if (!file) {
                clazz = ASMCodeGenerator.lookupClass(className);
                if (clazz == null) {
                    System.err.println("Warning: could not load class "
                            + className + ". Please check your classpath.");
                }
            } else {

                System.err.println("class name = " + className);
                try {
                    ASMRepository.parseClassFile(className.replace('.',
                            java.io.File.separatorChar)
                            + ".class");
                } catch (Exception e) {
                    System.err.println("got exception while loading class: "
                            + e);
                    System.exit(1);
                }
            }

            if (clazz != null) {
                if (ASMSerializationInfo.isSerializable(clazz)) {
                    if (!ASMSerializationInfo.isIbisSerializable(clazz)) {
                        addClass(clazz);
                    } else {
                        if (verbose) {
                            System.out.println(clazz.name
                                    + " already implements "
                                    + IBIS_IO_SERIALIZABLE);
                        }
                    }
                } else {
                    if (verbose) {
                        System.out.println(clazz.name
                                + " is not serializable");
                    }
                    ASMRepository.removeClass(clazz.name);
                }
            }
        }

        if (verbose) {
            System.out.println("Preparing classes");
        }

        for (int i = 0; i < classes_to_rewrite.size(); i++) {
            ClassNode clazz = classes_to_rewrite.get(i);
            addReferencesToRewrite(clazz);
        }

        ASMRepository.clearCache();

        /* Sort class to rewrite. Super classes first.  */
        do_sort_classes(classes_to_rewrite);

        for (int i = 0; i < classes_to_rewrite.size(); i++) {
            ClassNode clazz = classes_to_rewrite.get(i);
            if (useJME()) {
                // TODO new JMECodeGenerator(this, clazz).generateEmptyMethods();
            }
            else {
                new ASMCodeGenerator(this, clazz).generateEmptyMethods();
            }
        }

        if (verbose) {
            System.out.println("Rewriting classes");
        }

        /* Sort target_classes. Super classes first.  */
        do_sort_classes(target_classes);

        for (int i = 0; i < target_classes.size(); i++) {
            ClassNode clazz = target_classes.get(i);
            if ((clazz.access & ACC_INTERFACE) != ACC_INTERFACE) {
                if (!silent) {
                    System.out.println("  Rewrite class : "
                            + clazz.name);
                }
                if (useJME()) {
                    // TODO new JMECodeGenerator(this, clazz).generateCode();
                }
                else {
                    new ASMCodeGenerator(this, clazz).generateCode();
                }
            }
        }

        if (verbose) {
            System.out.println("Saving classes");
        }

        for (int i = 0; i < classes_to_save.size(); i++) {
            ClassNode clazz = classes_to_save.get(i);
            String cl = clazz.name;
            String classfile = "";

            try {
                if (local) {
                    int index = cl.lastIndexOf('/');
                    classfile = cl.substring(index + 1) + ".class";
                } else {
                    classfile = cl.replace('/', java.io.File.separatorChar)
                    + ".class";
                }
                if (verbose) {
                    System.out.println("  Saving class : " + classfile);
                }
               ASMRepository.dump(classfile, clazz);
            } catch (IOException e) {
                System.err.println("got exception while writing " + classfile
                        + ": " + e);
                e.printStackTrace(System.err);
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

        new ASMIOGenerator(verbose, local, file, force_generated_calls, 
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
