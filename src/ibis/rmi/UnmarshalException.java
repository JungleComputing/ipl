package ibis.rmi;

public class UnmarshalException extends RemoteException {

    public UnmarshalException(String s) {
	super(s);
    }

    public UnmarshalException(String s, Exception ex) {
	super(s, ex);
    }
}
