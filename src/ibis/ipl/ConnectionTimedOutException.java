package ibis.ipl;

import java.io.IOException;

public class ConnectionTimedOutException extends IOException {

    public ConnectionTimedOutException() {
	super();
    }

    public ConnectionTimedOutException(String name) {
	super(name);
    }

    public ConnectionTimedOutException(String name, Throwable cause) {
	super(name);
	initCause(cause);
    }

    public ConnectionTimedOutException(Throwable cause) {
	super();
	initCause(cause);
    }

}
