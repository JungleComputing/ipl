package ibis.rmi.registry;

import ibis.rmi.RemoteException;

public interface RegistryHandler
{
    public Registry registryStub(String host, int port) throws RemoteException;

    public Registry registryImpl(int port) throws RemoteException;
}
