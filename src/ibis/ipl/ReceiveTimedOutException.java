package ibis.ipl;

import java.io.IOException;

public class ReceiveTimedOutException extends IOException {

    public ReceiveTimedOutException() {
	super();
    }

    public ReceiveTimedOutException(String name) {
	super(name);
    }

    public ReceiveTimedOutException(String name, Throwable cause) {
	super(name);
	initCause(cause);
    }

    public ReceiveTimedOutException(Throwable cause) {
	super();
	initCause(cause);
    }

}
