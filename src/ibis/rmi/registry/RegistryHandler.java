package ibis.rmi.registry;

import ibis.rmi.RemoteException;
import ibis.rmi.UnknownHostException;

public interface RegistryHandler
{
    public Registry registryStub(String host, int port) throws RemoteException, UnknownHostException;

    public Registry registryImpl(int port) throws RemoteException;
}
