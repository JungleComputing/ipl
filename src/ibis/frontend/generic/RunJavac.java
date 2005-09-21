/* $Id$ */

package ibis.frontend.generic;

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
            javahome = javahome.substring(0, javahome.length()-4);
        }

        compiler = new String[] {
            javahome + filesep + "bin" + filesep + "javac",
            "-classpath", javapath + pathsep };
    }

    /**
     * Runs the Java compiler with the specified options on the specified
     * class.
     * @param options the compiler options
     * @param filename the Java file to compile
     * @param verbose if <code>true</code>, prints the compilation command on
     *   standard output
     * @return <code>true</code> if the exit status of the compiler is 0,
     *   <code>false</code> otherwise.
     */
    public static boolean runJavac(String[] options, String filename,
            boolean verbose) {
        try {
            RunProcess p;
            String[] cmd = new String[compiler.length + options.length + 1];
            for (int i = 0; i < compiler.length; i++) {
                cmd[i] = compiler[i];
            }
            for (int i = 0; i < options.length; i++) {
                cmd[compiler.length + i] = options[i];
            }

            cmd[compiler.length + options.length] = filename;

            if (verbose) {
                System.out.print("Running: ");
                for (int i = 0; i < cmd.length; i++) {
                    System.out.print(cmd[i] + " ");
                }
                System.out.println("");
            }
            p = new RunProcess(cmd, new String[0]);
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
