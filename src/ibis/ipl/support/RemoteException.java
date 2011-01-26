package ibis.ipl.support;

import java.io.IOException;

public class RemoteException extends IOException {

    public RemoteException() {
    }

    public RemoteException(String message) {
	super(message);
    }

    public RemoteException(Throwable cause) {
	super(cause);
    }

    public RemoteException(String message, Throwable cause) {
	super(message, cause);
    }

}
