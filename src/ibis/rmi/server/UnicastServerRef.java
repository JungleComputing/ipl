package ibis.rmi.server;

import ibis.rmi.RTS;
import ibis.rmi.Remote;
import ibis.rmi.RemoteException;

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
	    RemoteStub stub = RTS.exportObject(impl);
	    RemoteStub.setRef(stub, new UnicastRef(GUID));	    
	    return stub;
	} catch (Exception e) {
		if (RTS.DEBUG)
			e.printStackTrace();	
		throw new RemoteException(e.getMessage());
	}
    }


    public String getClientHost() throws ServerNotActiveException
    {
	// TODO:
	return null;
    }
}
