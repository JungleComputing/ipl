package ibis.rmi.impl;

import ibis.rmi.Remote;
import ibis.rmi.RemoteException;
import ibis.rmi.server.RemoteStub;
import ibis.rmi.server.ServerNotActiveException;
import ibis.rmi.server.ServerRef;

// TODO: implement getClientHost

public class UnicastServerRef extends UnicastRef implements ServerRef, java.io.Serializable
{
    public UnicastServerRef() 
    {
	GUID = "//" + RTS.getHostname() + "/" + (new java.rmi.server.UID()).toString();    
    }
    
    public RemoteStub exportObject(Remote impl, Object portData)
	throws RemoteException
    {
	try {
	    RemoteStub stub = RTS.exportObject(impl, new UnicastRef(GUID));
	    return stub;
	} catch (Exception e) {
		if (RTS.DEBUG) {
			e.printStackTrace();	
		}
		throw new RemoteException(e.getMessage(), e);
	}
    }


    public String getClientHost() throws ServerNotActiveException
    {
	// TODO:
	return null;
    }
}
