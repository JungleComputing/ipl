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

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class CheckNativeMethods {

    private static boolean first = true;

    private static void checkNativeMethods(JavaClass clazz) {

        // System.err.println("Inspect Class " + clazz);

        Method[] methods = clazz.getMethods();

        for (Method method : methods) {
            if (method.isNative()) {
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

    @SuppressWarnings("unused")
    public static void main(String[] args) {

        JavaClass[] clazz = null;
        boolean verbose = false;

        if (false) {
            System.err.print("CheckNativeMethods arguments: ");
            for (String arg : args) {
                System.err.print(arg + " ");
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
                    ClassParser p = new ClassParser(filename.replace('.', java.io.File.separatorChar));
                    clazz = increase_one(clazz);
                    clazz[clazz.length - 1] = p.parse();
                } catch (Exception e) {
                    System.err.println("Error for arg " + args[i] + ": " + e);
                    e.printStackTrace(System.err);
                    System.exit(1);
                }
            } else {
                clazz = increase_one(clazz);
                int index = args[i].lastIndexOf('.');
                try {
                    if (args[i].substring(index + 1).equals("class")) {
                        clazz[clazz.length - 1] = Repository.lookupClass(args[i].substring(0, index));
                    } else {
                        clazz[clazz.length - 1] = Repository.lookupClass(args[i]);
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Error: class " + args[i] + " not found");
                    System.exit(1);
                }
            }
        }

        if (clazz != null) {
            for (JavaClass element : clazz) {
                if (element != null) {
                    if (verbose) {
                        System.err.println("Check class " + element.getClassName());
                    }
                    checkNativeMethods(element);
                }
            }
            System.out.println("");
        }
    }

}
