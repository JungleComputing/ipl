package ibis.connect.util;

import java.io.OutputStream;
import java.io.PrintStream;

public class MyDebug {
    /** Some verbosity to trace protocols */
    private static final boolean enableVerbose;

    /** Really a lot of debug messages */
    private static final boolean enableDebug;

    public static boolean VERBOSE() {
        return enableVerbose;
    }

    public static boolean DEBUG() {
        return enableDebug;
    }

    public static void trace(String s) {
        if (enableVerbose) {
            System.err.println(s);
        }
    }

    public static void debug(String s) {
        if (enableDebug) {
            System.err.println(s);
        }
    }

    public static final PrintStream out;

    static {
        String sEnabled = System.getProperty(ConnProps.debug, "false");
        String sVerbose = System.getProperty(ConnProps.verbose, "false");

        enableVerbose = sVerbose.equals("true");
        enableDebug = sEnabled.equals("true");

        if (enableDebug) {
            System.err.println("MyDebug: traces enabled");
            out = System.err;
        } else {
            out = new PrintStream(new OutputStream() {
                public void write(int b) { /* discard data */
                }
            });
        }
    }

}