package ibis.util;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A class that allows you to postpone starting a program for a specified
 * number of seconds.
 * This may be useful under Windows, which does not have a sleep command.
 */
public class Postpone {

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
            System.err
                    .println("Usage: java ibis.util.Postpone <seconds> <main class> ...");
            System.exit(1);
        }
        int count = Integer.parseInt(args[0]);

        try {
            Thread.sleep(count * 1000);
        } catch (InterruptedException e) {
            // ignore;
        }

        // Now, load the class.
        Class cl = null;
        try {
            cl = Class.forName(args[1]);
        } catch (ClassNotFoundException e) {
            System.out.println("Could not load class " + args[1]);
            System.exit(1);
        }

        // Find the "main" method.
        Method m = null;
        try {
            m = cl.getMethod("main", new Class[] { args.getClass() });
        } catch (Exception e) {
            System.out.println("Could not find a main(String[]) in class "
                    + args[1]);
            System.exit(1);
        }

        // Create arguments array.
        String[] args2 = new String[args.length - 2];
        for (int i = 0; i < args2.length; i++) {
            args2[i] = args[i + 2];
        }

        // Make method accessible, so that it may be called.
        // Note that the main method is public, but the class
        // it lives in may not be.
        if (!m.isAccessible()) {
            final Method temporary_method = m;
            AccessController.doPrivileged(new PrivilegedAction() {
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