/* $Id$ */

package ibis.frontend.gmi;

import ibis.frontend.generic.BT_Analyzer;
import ibis.frontend.ibis.IbiscComponent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

public class Gmic extends IbiscComponent {

    private boolean local = true;

    // For Ibisc
    private boolean gmicEnabled = false;

    private JavaClass groupInterface;

    private ArrayList javaFiles = new ArrayList();

    public Gmic() {
        groupInterface = Repository.lookupClass("ibis.gmi.GroupInterface");

        if (groupInterface == null) {
            System.err.println("Class ibis.gmi.GroupInterface not found");
            System.exit(1);
        }
    }

    private String getFileName(JavaClass subject, String pkg, String name, String pre) {
        name = pre + name + ".java";
        if (!local && pkg != null && !pkg.equals("")) {
            return pkg.replace('.', File.separatorChar) + File.separator + name;
        }
        if (fromIbisc) {
            String dir = getDirectory(subject.getClassName());
            if (dir != null) {
                return dir + File.separator + name;
            }
        }
        return name;
    }

    private PrintWriter createFile(String name) throws Exception {

        File f = new File(name);

        if (!f.createNewFile()) {
            System.err.println("File " + name + " already exists!");
            System.exit(1);
        }

        FileOutputStream fileOut = new FileOutputStream(f);

        return new PrintWriter(fileOut);
    }

    public ArrayList processArgs(ArrayList args) {
        for (int i = 0; i < args.size(); i++) {
            String arg = (String) args.get(i);
            if (arg.equals("-gmi")) {
                gmicEnabled = true;
                args.remove(i--);
            }
        }
        return args;
    }

    public void process(Iterator iter) {
        if (! gmicEnabled) {
            return;
        }
        while (iter.hasNext()) {
            JavaClass subject = (JavaClass) iter.next();
            processFile(subject);

            // Facilitate for parameter vector, which may result in multiple
            // classes.
            if (javaFiles.size() > 0) {
                compile(javaFiles, subject.getClassName());
                javaFiles.clear();
            }
        }
    }

    public String rewriterImpl() {
        return "BCEL";
    }

    private void processFile(JavaClass subject) {
        try {
            PrintWriter output;
            String stub = null;
            String paramvec = null;
            String skeleton = null;

            if (verbose) {
                System.out.println("Handling " + subject.getClassName());
            }

            BT_Analyzer a = new BT_Analyzer(subject, groupInterface,
                    verbose);
            a.start(false);

            if (a.specialInterfaces.size() == 0) {
                return;
            }

            if (subject.isInterface()) {
                stub = getFileName(subject, a.packagename, a.classname, "group_stub_");
                output = createFile(stub);
                new GMIStubGenerator(a, output).generate();
                output.close();
                javaFiles.add(stub);

                paramvec = getFileName(subject, a.packagename, a.classname, "group_parametervector_");
                output = createFile(paramvec);
                new GMIParameterVectorGenerator(a, output).generate();
                output.close();
                javaFiles.add(paramvec);
            } else {
                skeleton = getFileName(subject, a.packagename, a.classname, "group_skeleton_");
                output = createFile(skeleton);
                new GMISkeletonGenerator(a, output).generate();
                output.close();
                javaFiles.add(skeleton);
            }
        } catch (Exception e) {
            System.err.println("Gmic got exception " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void doCompile(String[] args) {

        ArrayList classes = new ArrayList();        
        JavaClass groupInterface = null;

        if (args.length == 0) {
            System.err.println("Usage : java Gmic [-v] [-dir | -local] "
                    + "classname");
            System.exit(1);
        }


        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-v")) {
                verbose = true;
            } else if (args[i].equals("-dir")) {
                local = false;
            } else if (args[i].equals("-local")) {
                local = true;
            } else {
                String name = args[i];
                if (name.endsWith(".class")) {
                    name = name.substring(0, name.length()-6).replace(File.separatorChar, '.');
                }

                JavaClass c = Repository.lookupClass(name);
                if (c == null) {
                    System.err.println("Class " + name + " not found");
                    System.exit(1);
                }
                classes.add(c);
            }
        }

        for (int i = 0; i < classes.size(); i++) {
            JavaClass subject = (JavaClass) classes.get(i);
            processFile(subject);
        }
    }

    public static void main(String[] args) {
        new Gmic().doCompile(args);
    }
}
