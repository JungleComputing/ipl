package ibis.rmi;

import java.io.IOException;

public class RemoteException extends IOException
{
    public RemoteException() {}

    public RemoteException(String s) {
	super(s);
    }

    public RemoteException(String s, Throwable e) {
	super(s);
	initCause(e);
    }
}
