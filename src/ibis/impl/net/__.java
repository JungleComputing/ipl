package ibis.impl.net;

/**
 * Simple class for helper functions.
 */
public final class __ {

	/**
	 * Flag controlling {@link __#debug__(String) __.debug__(String)} output.
	 */
	public static boolean debugActivated__ = false;

	/**
	 * Aborts the process.
	 *
	 * @param msg the error message.
	 */
	public static final void abort__(String msg) {
		throw new Error(msg);
	}
	
	/**
	 * Aborts the process.
	 *
	 * This function reuses the message of a {@link Throwable}.
	 *
	 * @param e The throwable providing the error message.
	 */
	public static final void fwdAbort__(Throwable e) {
                e.printStackTrace();

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

	/**
	 * Indicate that the calling function is not currently implemented.
	 *
	 * This function allows to detect invalid calls of unimplemented functions.
	 * @param func the function name.
	 */
	public static final void unimplemented__(String func) {
		__.abort__(func + " unimplemented");
	}

	/**
	 * Display a string over the error output stream.
	 *
	 * @param s the string to display.
	 */
	public static void disp__(String s) {
		System.err.println(s);
	}

	/**
	 * Display a warning string over the error output stream.
	 *
	 * @param s the warning to display.
	 */
	public static void warning__(String s) {
		__.disp__("__warning__: " + s);
	}

	/**
	 * Display a string followed by a stack trace.
	 *
	 * @param s the string to display.
	 */
	public static void trace__(String s) {
		__.disp__(s);
		(new Throwable()).printStackTrace();
	}

	/**
	 * Display a debugging string over the error output stream.
	 * The string is sent to the error output stream only if
	 * {@link __#debugActivated__ __.debugActivated__} is set.
	 *
	 * @param s the string to display.
	 */
	public static void debug__(String s) {
		if (__.debugActivated__) {
			__.disp__(s);
		}
	}

        public static String state__(boolean b) {
                return b?"enabled":"disabled";
        }
        
}
