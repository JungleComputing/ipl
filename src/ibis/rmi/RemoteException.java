package ibis.rmi;

import java.io.IOException;

public class RemoteException extends IOException
{
    public Throwable detail;

    public RemoteException() {}

    public RemoteException(String s) {
	super(s);
    }

    public RemoteException(String s, Throwable e) {
	super(s);
	detail = e;
    }

    public String getMessage() {
	if (detail == null) 
	    return super.getMessage();
	
	return super.getMessage() + ":" + detail.toString();
    }
}
