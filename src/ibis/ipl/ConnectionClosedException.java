package ibis.ipl;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

public class ConnectionClosedException extends IOException {

    public ConnectionClosedException() {
	super();
    }

    public ConnectionClosedException(String name) {
	super(name);
    }

    public ConnectionClosedException(String name, Throwable cause) {
	super(name);
	initCause(cause);
    }

    public ConnectionClosedException(Throwable cause) {
	super();
	initCause(cause);
    }

}
