package ibis.rmi;

import ibis.rmi.RemoteException;

public class StubNotFoundException extends RemoteException {

    public StubNotFoundException(String s) {
	super(s);
    }

    public StubNotFoundException(String s, Exception ex) {
	super(s, ex);
    }
}
