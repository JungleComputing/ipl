package ibis.ipl.util.rpc;

import ibis.ipl.IbisIOException;

public class RemoteException extends IbisIOException {

    public RemoteException() {
	super();
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
