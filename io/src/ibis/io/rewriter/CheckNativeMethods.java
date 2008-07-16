/* $Id$ */

package ibis.io.rewriter;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class CheckNativeMethods {

    private static boolean first = true;

    private static void checkNativeMethods(JavaClass clazz) {

        // System.err.println("Inspect Class " + clazz);

        Method[] methods = clazz.getMethods();

        for (int i = 0; i < methods.length; i++) {
            if (methods[i].isNative()) {
                if (first) {
                    first = false;
                } else {
                    System.out.print(",");
                }
                System.out.print(clazz.getClassName());
                return;
            }
        }
    }

    private static JavaClass[] increase_one(JavaClass[] old) {
        JavaClass[] s;
        if (old == null) {
            s = new JavaClass[1];
        } else {
            s = new JavaClass[old.length + 1];
            for (int i = 0; i < old.length; i++) {
                s[i] = old[i];
            }
        }
        return s;
    }

    public static void main(String[] args) {

        JavaClass[] clazz = null;
        boolean verbose = false;

        if (false) {
            System.err.print("CheckNativeMethods arguments: ");
            for (int i = 0; i < args.length; i++) {
                System.err.print(args[i] + " ");
            }
            System.err.println();
        }

        for (int i = 0; i < args.length; i++) {
            if (false) { /* do nothing */
            } else if (args[i].equals("-v")) {
                verbose = true;
            } else if (args[i].equals("-f")) {
                String filename = args[++i];

                try {
                    ClassParser p = new ClassParser(filename.replace('.',
                            java.io.File.separatorChar));
                    clazz = increase_one(clazz);
                    clazz[clazz.length - 1] = p.parse();
                } catch (Exception e) {
                    System.err.println("Error for arg " + args[i] + ": " + e);
                }
            } else {
                clazz = increase_one(clazz);
                int index = args[i].lastIndexOf('.');
                if (args[i].substring(index + 1).equals("class")) {
                    clazz[clazz.length - 1]
                        = Repository.lookupClass(args[i].substring(0, index));
                } else {
                    clazz[clazz.length - 1] = Repository.lookupClass(args[i]);
                }
            }
        }

        if (clazz != null) {
            for (int i = 0; i < clazz.length; i++) {
                if (clazz[i] != null) {
                    if (verbose) {
                        System.err.println("Check class "
                                + clazz[i].getClassName());
                    }
                    checkNativeMethods(clazz[i]);
                }
            }
            System.out.println("");
        }
    }

}
