package ibis.rmi.server;

import ibis.rmi.*;

public interface ServerRef extends RemoteRef {

    public RemoteStub exportObject(Remote obj, Object data) throws RemoteException;

    public String getClientHost() throws ServerNotActiveException;
}
