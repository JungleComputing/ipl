package ibis.rmi.registry.impl;

import ibis.rmi.*;
import ibis.rmi.registry.*;

public class RegistryHandler implements ibis.rmi.registry.RegistryHandler {

    public Registry registryStub(String host, int port) 
	throws RemoteException, UnknownHostException 
    {	
	return (Registry) new RegistryImpl(host, port);
    }
    
    public Registry registryImpl(int port)
	throws RemoteException
    {
	return (Registry) new RegistryImpl(port);
    }
}
