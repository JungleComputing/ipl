package ibis.ipl;

import java.io.PrintStream;
import java.io.PrintWriter;

public class IbisException extends java.lang.Exception {
	// this space was intensionally left blank, but is now taken...

    public IbisException() {
	super();
    }

    public IbisException(String name) {
	super(name);
    }

    public IbisException(String name, Throwable cause) {
	super(name);
	initCause(cause);
    }

    public IbisException(Throwable cause) {
	super();
	initCause(cause);
    }

}
