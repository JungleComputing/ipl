package ibis.rmi;

public class ServerError extends RemoteException {
    
    public ServerError(String s, Error e) {
	super(s, e);
    }
}
