package ibis.rmi.registry.impl;

import ibis.rmi.RemoteException;
import ibis.rmi.registry.Registry;

public class RegistryHandler implements ibis.rmi.registry.RegistryHandler {

    public Registry registryStub(String host, int port) 
    {	
	return (Registry) new RegistryImpl(host, port);
    }
    
    public Registry registryImpl(int port)
	throws RemoteException
    {
	return (Registry) new RegistryImpl(port);
    }
}
