package ibis.connect.util;

import java.io.OutputStream;
import java.io.PrintStream;

public class MyDebug
{
    /** Some verbosity to trace protocols */
    private static boolean enableVerbose = false;

    /** Really a lot of debug messages */
    private static boolean enableDebug   = false;

    public static boolean VERBOSE() {
	return enableVerbose;
    }

    public static boolean DEBUG() {
	return enableDebug;
    }

    public static void trace(String s) {
	if(enableVerbose) {
	    System.err.println(s);
	}
    }

    public static void debug(String s) {
	if(enableDebug) {
	    System.err.println(s);
	}
    }

    public static PrintStream out = System.err; /* needs a non-null default, ifever a class has 
						   trace messages in its static initializer */
    
    static {
	String sEnabled = System.getProperty("ibis.connect.debug", "false");
	enableDebug = sEnabled.equals("true");

	String sVerbose = System.getProperty("ibis.connect.verbose", "false");
	enableVerbose = sVerbose.equals("true");

	if(enableDebug)
	    {
		System.err.println("MyDebug: traces enabled");
		MyDebug.out = System.err;
	    }
	else
	    {
		MyDebug.out = new PrintStream(new OutputStream() {
			public void write(int b) { /* discard data */ } 
		    });
	    }
    }

}
