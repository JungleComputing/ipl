package ibis.connect.util;

import java.io.OutputStream;
import java.io.PrintStream;

public class MyDebug
{
    public static PrintStream out = System.out; /* needs a non-null default, ifever a class has 
						   trace messages in its statis initializer */
    
    static {
	String sEnabled = System.getProperty("alex.debug", "false");
	boolean enabled = sEnabled.equals("true");
	//boolean enabled = true;
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
