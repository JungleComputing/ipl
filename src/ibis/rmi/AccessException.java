package ibis.rmi;

public class AccessException extends RemoteException
{
    public AccessException(String s) {
	super(s);
    }

    public AccessException(String s, Exception e) {
	super(s, e);
    }
}
