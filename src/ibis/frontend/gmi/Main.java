package ibis.frontend.gmi;

import ibis.frontend.generic.BT_Analyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Vector;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

class Main {

    static boolean local = true;

    public static String getFileName(String pkg, String name, String pre) {
        if (!local && pkg != null && !pkg.equals("")) {
            return pkg.replace('.', '/') + '/' + pre + name + ".java";
        }
        return (pre + name + ".java");
    }

    public static PrintWriter createFile(String name) throws Exception {

        File f = new File(name);

        if (!f.createNewFile()) {
            System.err.println("File " + name + " already exists!");
            System.exit(1);
        }

        FileOutputStream fileOut = new FileOutputStream(f);

        return new PrintWriter(fileOut);
    }

    public static void main(String[] args) {

        Vector classes = new Vector();
        boolean verbose = false;
        JavaClass groupInterface = null;

        if (args.length == 0) {
            System.err.println("Usage : java Main [-v] [-dir | -local] "
                    + "classname");
            System.exit(1);
        }

        groupInterface = Repository.lookupClass("ibis.gmi.GroupInterface");

        if (groupInterface == null) {
            System.err.println("Class ibis.gmi.GroupInterface not found");
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
                JavaClass c = Repository.lookupClass(args[i]);
                if (c == null) {
                    System.err.println("Class " + args[i] + " not found");
                    System.exit(1);
                }
                classes.addElement(c);
            }
        }

        for (int i = 0; i < classes.size(); i++) {

            try {
                PrintWriter output;
                JavaClass subject = (JavaClass) classes.get(i);

                if (verbose) {
                    System.out.println("Handling " + subject.getClassName());
                }

                BT_Analyzer a = new BT_Analyzer(subject, groupInterface,
                        verbose);
                a.start();

                if (a.specialInterfaces.size() == 0) {
                    continue;
                }

                if (subject.isInterface()) {
                    output = createFile(getFileName(a.packagename, a.classname,
                            "group_stub_"));
                    new GMIStubGenerator(a, output).generate();
                    output.flush();

                    output = createFile(getFileName(a.packagename, a.classname,
                            "group_parametervector_"));
                    new GMIParameterVectorGenerator(a, output).generate();
                    output.flush();
                } else {
                    output = createFile(getFileName(a.packagename, a.classname,
                            "group_skeleton_"));
                    new GMISkeletonGenerator(a, output).generate();
                    output.flush();
                }

            } catch (Exception e) {
                System.err.println("Main got exception " + e);
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
