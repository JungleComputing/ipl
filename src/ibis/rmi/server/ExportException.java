package ibis.rmi.server;

import ibis.rmi.RemoteException;

public class ExportException extends RemoteException
{
    public ExportException(String s) {
	super(s);
    }

    public ExportException(String s, Exception e) {
	super(s, e);
    }
}
