package ibis.rmi;

public class ServerRuntimeException extends RemoteException {
    public ServerRuntimeException(String s, Exception ex) {
	super(s, ex);
    }
}
