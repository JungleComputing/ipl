package ibis.rmi;

public class MarshalException extends RemoteException {

    public MarshalException(String s) {
	super(s);
    }

    public MarshalException(String s, Exception ex) {
	super(s, ex);
    }
}
