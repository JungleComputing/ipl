package ibis.rmi;

public class UnknownHostException extends RemoteException
{
    public UnknownHostException(String s) {
	super(s);
    }

    public UnknownHostException(String s, Exception e) {
	super(s, e);
    }
}
