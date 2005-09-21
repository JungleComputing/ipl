/* $Id$ */

package ibis.frontend.repmi;

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
        JavaClass repmiInterface = null;

        if (args.length == 0) {
            System.err.println("Usage : java Main [-v] [-dir | -local] "
                    + "classname");
            System.exit(1);
        }

        int num = args.length;
        int i = 0;

        while (i < num) {
            if (args[i].equals("-v")) {
                verbose = true;
                args[i] = args[num - 1];
                num--;
            } else if (args[i].equals("-dir")) {
                local = false;
                args[i] = args[num - 1];
                num--;
            } else if (args[i].equals("-local")) {
                local = true;
                args[i] = args[num - 1];
                num--;
            } else {
                i++;
            }
        }

        repmiInterface = Repository.lookupClass("ibis.repmi.ReplicatedMethods");

        if (repmiInterface == null) {
            System.err.println("Class ibis.repmi.ReplicatedMethods not found");
            System.exit(1);
        }

        for (i = 0; i < num; i++) {
            JavaClass c = Repository.lookupClass(args[i]);
            if (c == null) {
                System.err.println("Class " + args[i] + " not found");
                System.exit(1);
            }
            classes.addElement(c);
        }

        for (i = 0; i < classes.size(); i++) {

            try {
                PrintWriter output;
                JavaClass subject = (JavaClass) classes.get(i);

                if (verbose) {
                    System.out.println("Handling " + subject.getClassName());
                }

                BT_Analyzer a = new BT_Analyzer(subject, repmiInterface,
                        verbose);
                a.start(false);

                if (a.subjectSpecialMethods != null) {
                    output = createFile(getFileName(a.packagename, a.classname,
                            "repmi_stub_"));
                    new RepMIStubGenerator(a, output, verbose).generate();
                    output.flush();

                    output = createFile(getFileName(a.packagename, a.classname,
                            "repmi_skeleton_"));
                    new RepMISkeletonGenerator(a, output).generate();
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
