package ibis.connect.util;

import java.io.OutputStream;
import java.io.PrintStream;

public class MyDebug
{
    public static boolean DEBUG = false;
    public static boolean VERBOSE = false;

    public static PrintStream out = System.out; /* needs a non-null default, ifever a class has 
						   trace messages in its static initializer */
    
    static {
	String sEnabled = System.getProperty("ibis.connect.debug", "false");
	boolean enabled = sEnabled.equals("true");

	String sVerbose = System.getProperty("ibis.connect.verbose", "false");
	boolean verbose = sVerbose.equals("true");

	VERBOSE = verbose;
	DEBUG = enabled;

	if(enabled)
	    {
		System.out.println("MyDebug: traces enabled");
		MyDebug.out = System.out;
	    }
	else
	    {
		MyDebug.out = new PrintStream(new OutputStream() {
			public void write(int b) { /* discard data */ } 
		    });
	    }
    }

}
