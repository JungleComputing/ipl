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

package ibis.compile.util;

import ibis.util.RunProcess;

/**
 * This class exports a method to run the java compiler on a specified class.
 */
public class RunJavac {

    private static String[] compiler;

    static {
        String javahome = System.getProperty("java.home");
        String javapath = System.getProperty("java.class.path");
        String filesep = System.getProperty("file.separator");
        String pathsep = System.getProperty("path.separator");

        if (javahome.endsWith("jre")) {
            // IBM java does this
            javahome = javahome.substring(0, javahome.length() - 4);
        }

        compiler = new String[] { javahome + filesep + "bin" + filesep + "javac", "-classpath", javapath + pathsep };
    }

    /**
     * Sets the compiler.
     * 
     * @param c compiler name plus options
     */
    public static void setCompiler(String[] c) {
        compiler = c;
    }

    /**
     * Runs the Java compiler with the specified options on the specified class.
     * 
     * @param compilerArgs the compiler arguments
     * @param verbose      if <code>true</code>, prints the compilation command on
     *                     standard output
     * @return <code>true</code> if the exit status of the compiler is 0,
     *         <code>false</code> otherwise.
     */
    public static boolean runJavac(String[] compilerArgs, boolean verbose) {
        try {
            RunProcess p;
            String[] cmd = new String[compiler.length + compilerArgs.length];
            for (int i = 0; i < compiler.length; i++) {
                cmd[i] = compiler[i];
            }
            for (int i = 0; i < compilerArgs.length; i++) {
                cmd[compiler.length + i] = compilerArgs[i];
            }

            if (verbose) {
                System.out.print("Running: ");
                for (String element : cmd) {
                    System.out.print(element + " ");
                }
                System.out.println("");
            }
            p = new RunProcess(cmd);
            p.run();
            int res = p.getExitStatus();
            byte[] err = p.getStderr();
            byte[] out = p.getStdout();
            if (out.length != 0) {
                System.out.write(out, 0, out.length);
                System.out.println("");
            }
            if (err.length != 0) {
                System.err.write(err, 0, err.length);
                System.err.println("");
            }
            if (res != 0) {
                return false;
            }
        } catch (Exception e) {
            System.err.println("IO error: " + e);
            e.printStackTrace();
            System.exit(1);
        }
        return true;
    }
}
