/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    Vector<JavaClass> classes_to_rewrite, target_classes, classes_to_save;

    HashMap<String, JavaClass> arguments;

    boolean fromIbisc = false;

    public IOGenerator() {
        classes_to_rewrite = new Vector<>();
        target_classes = new Vector<>();
        classes_to_save = new Vector<>();

        arguments = new HashMap<>();

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

    public IOGenerator(boolean verbose, boolean local, boolean file, boolean force_generated_calls, boolean silent) {
        this();
        this.verbose = verbose;
        this.local = local;
        this.file = file;
        this.force_generated_calls = force_generated_calls;
        this.silent = silent;
    }

    @Override
    public boolean processArgs(ArrayList<String> args) {
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.equals("-iogen-force")) {
                force_generated_calls = true;
                args.remove(i);
                i--;
            }
        }
        return true;
    }

    @Override
    public String getUsageString() {
        return "[-iogen-force]";
    }

    @Override
    public void process(Iterator<?> classes) {
        fromIbisc = true;
        arguments = new HashMap<>();
        for (Iterator<?> i = classes; i.hasNext();) {
            JavaClass cl = (JavaClass) i.next();
            arguments.put(cl.getClassName(), cl);
        }
        for (JavaClass cl : arguments.values()) {
            if (SerializationInfo.isSerializable(cl)) {
                if (!SerializationInfo.isIbisSerializable(cl)) {
                    addClass(cl);
                }
            }
        }
        for (JavaClass clazz : classes_to_rewrite) {
            addReferencesToRewrite(clazz);
        }

        /* Sort class to rewrite. Super classes first. */
        do_sort_classes(classes_to_rewrite);

        for (JavaClass clazz : classes_to_rewrite) {
            new CodeGenerator(this, clazz).generateEmptyMethods();
        }

        if (verbose) {
            System.out.println("Ibisc: IOGenerator rewriting classes");
        }

        /* Sort target_classes. Super classes first. */
        do_sort_classes(target_classes);

        for (JavaClass clazz : target_classes) {
            if (verbose) {
                System.out.println("Target Class: " + clazz.getClassName());
            }
            if (!clazz.isInterface()) {
                if (!silent) {
                    System.out.println("  Rewrite class : " + clazz.getClassName());
                }
                new CodeGenerator(this, clazz).generateCode();
            }
        }
    }

    @Override
    public String rewriterImpl() {
        return "BCEL";
    }

    private void addTargetClass(JavaClass clazz) {
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

    private void addRewriteClass(Type t, JavaClass clazz) {
        if (t instanceof ArrayType) {
            addRewriteClass(((ArrayType) t).getBasicType(), clazz);
        } else if (t instanceof ObjectType) {
            String name = ((ObjectType) t).getClassName();
            JavaClass c = CodeGenerator.lookupClass(name);
            if (c != null) {
                if (!local || clazz.getPackageName().equals(c.getPackageName())) {
                    addClass(c);
                }
            }
        }
    }

    private void addRewriteClass(JavaClass clazz) {
        if (!classes_to_rewrite.contains(clazz) && !SerializationInfo.isIbisSerializable(clazz)) {
            classes_to_rewrite.add(clazz);

            /*
             * if (clazz.getMajor() > 50) { clazz.setMajor(50); // Dirty hack as long as we
             * don't generate StackMap. }
             */

            if (verbose) {
                System.out.println("Adding rewrite class : " + clazz.getClassName());
            }
        }
    }

    private void addClass(JavaClass clazz) {
        boolean serializable = false;

        if (!clazz.isClass() || clazz.getClassName().equals(TYPE_JAVA_LANG_CLASS) || clazz.getClassName().equals(TYPE_JAVA_LANG_STRING)) {
            return;
        }

        try {
            if (Repository.instanceOf(clazz, TYPE_JAVA_LANG_ENUM)) {
                return;
            }
        } catch (Exception e) {
            // Sigh: BCEL throws a NullPointerException if java.lang.Enum
            // does not exist
        }

        if (!classes_to_rewrite.contains(clazz)) {

            JavaClass super_classes[];

            try {
                super_classes = Repository.getSuperClasses(clazz);
            } catch (ClassNotFoundException e) {
                super_classes = null;
            }

            if (super_classes != null) {
                for (JavaClass super_Class : super_classes) {
                    if (SerializationInfo.isSerializable(super_Class)) {
                        serializable = true;
                        if (!SerializationInfo.isIbisSerializable(super_Class)) {
                            if (!local || clazz.getPackageName().equals(super_Class.getPackageName())) {
                                addRewriteClass(super_Class);
                            }
                        } else if (verbose) {
                            System.out.println(clazz.getClassName() + " already implements " + TYPE_IBIS_IO_SERIALIZABLE);
                        }
                    }
                }
            }
            serializable |= SerializationInfo.isSerializable(clazz);
        } else {
            serializable = true;
        }

        if (serializable) {
            addRewriteClass(clazz);
            addTargetClass(clazz);
        }
    }

    void addReferencesToRewrite(JavaClass clazz) {

        /*
         * Find all references to final reference types and add these to the rewrite
         * list
         */
        Field[] fields = clazz.getFields();

        for (Field field : fields) {
            /* Don't send fields that are STATIC or TRANSIENT */
            if (!(field.isStatic() || field.isTransient())) {
                Type field_type = Type.getType(field.getSignature());

                if (!(field_type instanceof BasicType) && (field_type != Type.STRING) && SerializationInfo.isFinal(field_type)) {
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

        /*
         * do the following here....
         *
         * for each of the classes in args
         *
         * - load it. - scan to see if it's parent is serializable - if so, add parent
         * to rewrite list - scan to see if it is serializable - if so, add to rewrite
         * list
         *
         * for each of the classes in the rewrite list
         *
         * - check if it contains references to final serializable objects - if so, add
         * these objects to the rewrite list - check if it already extends
         * ibis.io.Serializable - if not, add it and add the neccesary methods (empty) -
         * check if it is a target - if so, add it to the target list
         *
         * for each of the objects on the target list
         *
         * - generate the code for the methods - save the class file
         *
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
                    System.err.println("Warning: could not load class " + className + ". Please check your classpath.");
                }
            } else {

                System.err.println("class name = " + className);
                try {
                    ClassParser p = new ClassParser(className.replace('.', java.io.File.separatorChar) + ".class");
                    clazz = p.parse();
                    if (clazz != null) {
                        Repository.removeClass(className);
                        Repository.addClass(clazz);
                    }
                } catch (Exception e) {
                    System.err.println("got exception while loading class: " + e);
                    System.exit(1);
                }
            }

            if (clazz != null) {
                if (SerializationInfo.isSerializable(clazz)) {
                    if (!SerializationInfo.isIbisSerializable(clazz)) {
                        addClass(clazz);
                    } else if (verbose) {
                        System.out.println(clazz.getClassName() + " already implements " + TYPE_IBIS_IO_SERIALIZABLE);
                    }
                } else {
                    if (verbose) {
                        System.out.println(clazz.getClassName() + " is not serializable");
                    }
                    Repository.removeClass(clazz);
                }
            }
        }

        if (verbose) {
            System.out.println("Preparing classes");
        }

        for (JavaClass clazz : classes_to_rewrite) {
            addReferencesToRewrite(clazz);
        }

        Repository.clearCache();

        /* Sort class to rewrite. Super classes first. */
        do_sort_classes(classes_to_rewrite);

        for (JavaClass clazz : classes_to_rewrite) {
            new CodeGenerator(this, clazz).generateEmptyMethods();
        }

        if (verbose) {
            System.out.println("Rewriting classes");
        }

        /* Sort target_classes. Super classes first. */
        do_sort_classes(target_classes);

        for (JavaClass clazz : target_classes) {
            if (!clazz.isInterface()) {
                if (!silent) {
                    System.out.println("  Rewrite class : " + clazz.getClassName());
                }
                new CodeGenerator(this, clazz).generateCode();
            }
        }

        if (verbose) {
            System.out.println("Saving classes");
        }

        for (JavaClass clazz : classes_to_save) {
            String cl = clazz.getClassName();
            String classfile = "";

            try {
                if (local) {
                    int index = cl.lastIndexOf('.');
                    classfile = cl.substring(index + 1) + ".class";
                } else {
                    classfile = cl.replace('.', java.io.File.separatorChar) + ".class";
                }
                if (verbose) {
                    System.out.println("  Saving class : " + classfile);
                }
                clazz.dump(classfile);
            } catch (IOException e) {
                System.err.println("got exception while writing " + classfile + ": " + e);
                System.exit(1);
            }
        }
    }

    private static void usage() {
        System.out.println("Usage : java IOGenerator [-dir|-local] " + "[-package <package>] [-v]" + "<fully qualified classname list | classfiles>");
        System.exit(1);
    }

    public static void main(String[] args) {
        boolean verbose = false;
        boolean local = true;
        boolean file = false;
        boolean force_generated_calls = false;
        boolean silent = false;
        Vector<String> files = new Vector<>();
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
            } else if (args[i].equals("-package")) {
                pack = args[i + 1];
                i++; // skip arg
            } else {
                usage();
            }
        }

        Vector<String> newArgs = new Vector<>();
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

        new IOGenerator(verbose, local, file, force_generated_calls, silent).scanClass(newArgs);
    }

    private static void processDirectory(File f, Vector<String> args, String name) {
        File[] list = f.listFiles();
        String prefix = "";

        if (!name.equals(".")) {
            prefix = name + ".";
        }
        for (File element : list) {
            String fname = element.getName();
            if (element.isDirectory()) {
                processDirectory(element, args, prefix + fname);
            } else {
                int index = fname.lastIndexOf(".class");
                if (index != -1) {
                    args.add(prefix + fname.substring(0, index));
                }
            }
        }
    }
}
