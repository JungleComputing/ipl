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

package ibis.util;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A class that allows you to postpone starting a program for a specified
 * number of seconds.
 * This may be useful under Windows, which does not have a sleep command.
 */
public class Postpone {

    private Postpone() {
        // prevent construction.
    }

    /**
     * Main program.
     * It finds the <code>main</code> method in the class specified by
     * the second argument, and invokes it with the rest of the arguments
     * after sleeping for the number of seconds specified by the first
     * argument.
     * @param args the array of arguments. The first argument specifies
     * the number of seconds to sleep, the second argument specifies the
     * name of the class whose <code>main</code> method must be invoked,
     * the rest of the arguments are passed on to the <code>main</code>
     * invocation.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ibis.util.Postpone "
                    + "[ -o <outputfile> ] <seconds> <main class> ...");
            System.exit(1);
        }

        int count = Integer.parseInt(args[0]);
        int argsbase = 1;

        if (args[1].equals("-o")) {
            if (args.length < 4) {
                throw new Error("Usage: java ibis.util.Postpone "
                        + "[ -o <outputfile> ] <seconds> <main class> ...");
            }
            String outfile = args[2];
            try {
                FileOutputStream s = new FileOutputStream(outfile);
                PrintStream p = new PrintStream(s);
                System.setErr(p);
                System.setOut(p);
            } catch(Exception e) {
                System.err.println("Could not write file " + outfile);
                System.exit(1);
            }
            argsbase = 3;
        }

        try {
            Thread.sleep(count * 1000);
        } catch (InterruptedException e) {
            // ignore;
        }

        // Now, load the class.
        Class<?> cl = null;
        try {
            cl = Class.forName(args[argsbase]);
        } catch (ClassNotFoundException e) {
            try {
                cl = Thread.currentThread().getContextClassLoader()
                        .loadClass(args[argsbase]);
            } catch(ClassNotFoundException e2) {
                throw new Error("Could not load class " + args[argsbase]);
            }
        }

        // Find the "main" method.
        Method m = null;
        try {
            m = cl.getMethod("main", new Class[] { args.getClass() });
        } catch (Exception e) {
            throw new Error("Could not find a main(String[]) in class "
                    + args[argsbase]);
        }

        // Create arguments array.
        String[] args2 = new String[args.length - argsbase-1];
        for (int i = 0; i < args2.length; i++) {
            args2[i] = args[i + argsbase + 1];
        }

        // Make method accessible, so that it may be called.
        // Note that the main method is public, but the class
        // it lives in may not be.
        if (!m.isAccessible()) {
            final Method temporary_method = m;
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    temporary_method.setAccessible(true);
                    return null;
                }
            });
        }

        // Invoke
        try {
            m.invoke(null, new Object[] { args2 });
        } catch (Exception e) {
            System.out.println("Could not invoke main: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
