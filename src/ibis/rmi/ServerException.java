package ibis.rmi;

public class ServerException extends RemoteException {

    public ServerException(String s) {
	super(s);
    }

    public ServerException(String s, Exception ex) {
	super(s, ex);
    }
}
