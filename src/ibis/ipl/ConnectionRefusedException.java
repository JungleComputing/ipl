package ibis.ipl;

import java.io.IOException;

public class ConnectionRefusedException extends IOException {

    public ConnectionRefusedException() {
	super();
    }

    public ConnectionRefusedException(String name) {
	super(name);
    }

    public ConnectionRefusedException(String name, Throwable cause) {
	super(name);
	initCause(cause);
    }

    public ConnectionRefusedException(Throwable cause) {
	super();
	initCause(cause);
    }

}
