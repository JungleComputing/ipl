package ibis.ipl.impl.net;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

// Simple class for helper functions
public final class __ {
	public static boolean debugActivated__ = false;

	//
	public static final void abort__(String msg) {
		throw new Error(msg);
	}
	
	public static final void fwdAbort__(Throwable e) {
		throw new Error(e.getMessage());
		// Java >= 1.4 only: throw new Error(e);
	}
	
	/* Java >= 1.4 only  :-(
	public static final void unimplemented__() {
		String func =
			((new Throwable()).getStackTrace())[1].getMethodName();
		__.abort__(func + " unimplemented");
	}
	*/

	public static final void unimplemented__(String func) {
		__.abort__(func + " unimplemented");
	}

	public static void disp__(String s) {
		System.err.println(s);
	}

	public static void warning__(String s) {
		__.disp__("__warning__: " + s);
	}

	public static void trace__(String s) {
		__.disp__(s);
		(new Throwable()).printStackTrace();
	}

	public static void debug__(String s) {
		if (__.debugActivated__) {
			__.disp__(s);
		}
	}
	
}
