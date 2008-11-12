/* $Id$ */

package ibis.io.rewriter;

import ibis.util.RunProcess;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class Failed {
    String[] class_names;

    StringBuffer error;

    int exit;
}

public class Scan {

    public static final int BATCH_SIZE = 20;

    private static Vector<String> classes = new Vector<String>();

    private static Vector<Failed> failed = new Vector<Failed>();

    public static String[] getClassPath() {
        int i = 0;

        String cp = System.getProperty("java.class.path");
        StringTokenizer st = new StringTokenizer(cp, File.pathSeparator);
        String[] entries = new String[st.countTokens()];

        while (st.hasMoreTokens()) {
            entries[i++] = st.nextToken();
        }
        return entries;
    }

    public static String path2qualified(String name) {

        // remove the '.class' part
        if (name.endsWith(".class")) {
            name = name.substring(0, name.length() - 6);
        }

        name = name.replace('/', '.');
        return name;
    }

    public static void handleClasses(String[] class_names) {
        int result = 0;
        RunProcess p = null;

        try {
            String command = "java ibis.io.rewriter.IOGenerator ";

            for (int i = 0; i < BATCH_SIZE; i++) {
                if (class_names[i] != null) {
                    command += class_names[i] + " ";
                }
            }

            //	    System.out.println("Command = " + command);

            p = new RunProcess(command);
            p.run();
            result = p.getExitStatus();
        } catch (Exception e) {
            result = -1;
        }

        if (result != 0) {
            Failed f = new Failed();

            f.class_names = class_names;
            f.error = new StringBuffer("");
            f.exit = result;

            f.error.append("stdout:\n");
            f.error.append("      : ");

            if (p != null) {
                byte[] o = p.getStdout();
                for (int i = 0; i < o.length; i++) {
                    f.error.append(((char) o[i]));
                    if (o[i] == '\n') {
                        f.error.append("      : ");
                    }
                }
            }

            f.error.append("\nstderr: \n");
            f.error.append("      : ");

            if (p != null) {
                byte[] e = p.getStderr();
                for (int i = 0; i < e.length; i++) {
                    f.error.append(((char) e[i]));
                    if (e[i] == '\n') {
                        f.error.append("      : ");
                    }
                }
            }

            failed.add(f);
        }
    }

    public static void scanJar(String jarfile) throws IOException {
        JarFile jarf = new JarFile(jarfile);
        Enumeration<JarEntry> e = jarf.entries();

        while (e.hasMoreElements()) {
            JarEntry entry = e.nextElement();

            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                classes.add(path2qualified(entry.getName()));
            }
        }

        jarf.close();
        System.out.println("Jarfile " + jarfile + " contains " + classes.size()
                + " classes.");

        System.out.println("Starting rewrite");

        long start = System.currentTimeMillis();
        int total = classes.size();
        String[] batch = new String[BATCH_SIZE];

        for (int i = 0; i < total; i += BATCH_SIZE) {
            for (int j = 0; j < BATCH_SIZE; j++) {
                if (i + j < total) {
                    batch[j] = classes.get(i + j);
                    System.out.println(batch[j] + " (" + (i + j) + " of "
                            + total + ")");
                } else {
                    batch[j] = null;
                }
            }

            long temp1 = System.currentTimeMillis();

            handleClasses(batch);

            long temp2 = System.currentTimeMillis();

            System.out.println("Handled in " + ((temp2 - temp1) / 1000.0)
                    + " seconds.");
        }

        long end = System.currentTimeMillis();

        System.out.println("Done rewrite in " + ((end - start) / 1000.0)
                + " seconds.");

        total = failed.size();
        if (total > 0) {
            System.out.println("The following batch failed to rewrite "
                    + "properly:");

            for (int i = 0; i < total; i++) {
                Failed f = failed.get(i);

                for (int j = 0; j < BATCH_SIZE; j++) {
                    System.out.println(f.class_names[j]);
                }

                System.out.println("errorcode " + f.exit + "\n"
                        + f.error.toString());
            }
        }
    }

    public static void main(String[] args) {
        try {
            if (args[0].endsWith(".jar")) {
                scanJar(args[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
