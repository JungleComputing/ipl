package ibis.rmi;

import java.io.IOException;

public class RemoteException extends ibis.ipl.IbisIOException
{
	public RemoteException() {
		super();
	}

	public RemoteException(String name) {
		super(name);
	}

	public RemoteException(String name, Throwable cause) {
		super(name);
		initCause(cause);
	}

	public RemoteException(Throwable cause) {
		super();
		initCause(cause);
	}
}
