package ibis.rmi.server;

import ibis.rmi.Remote;
import ibis.rmi.RemoteException;

public interface ServerRef extends RemoteRef {

    public RemoteStub exportObject(Remote obj, Object data) throws RemoteException;

    public String getClientHost() throws ServerNotActiveException;
}
