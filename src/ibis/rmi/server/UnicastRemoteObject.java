package ibis.rmi.server;

import ibis.rmi.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ClassNotFoundException;
import java.lang.CloneNotSupportedException;

public class UnicastRemoteObject extends RemoteServer
{
    protected UnicastRemoteObject() throws RemoteException
    {
	exportObject((Remote)this);
    }

    public Object clone() throws CloneNotSupportedException
    {
	try {
	    UnicastRemoteObject r = (UnicastRemoteObject)(super.clone());
	    r.reexport();
	    return r;
	} catch (RemoteException e) {
	    throw new CloneNotSupportedException("Clone failed: " + e.toString());
	}
    }

    public static RemoteStub exportObject(Remote obj) throws RemoteException
    {
	// Use exportObject from the "current" UnicastServerRef".
	    /*Class cl = Class.forName(RemoteRef.packagePrefix + ".UnicastServerRef");
	    Object uref = cl.newInstance();
	    if (uref instanceof ServerRef) {
		ServerRef ref = (ServerRef)uref;
		if (obj instanceof UnicastRemoteObject) {
		    ((UnicastRemoteObject)obj).ref = ref;
		}
		return ref.exportObject(obj, null);
	    }

	    throw new ExportException(RemoteRef.packagePrefix + ".UnicastServerRef is not a ServerRef");*/
	    ServerRef ref = new UnicastServerRef();
	    if (obj instanceof RemoteObject) {
		if (((RemoteObject)obj).ref != null) {
		    throw new ExportException("object already exported");
		}
		((RemoteObject)obj).ref = ref;
	    }
	    return ref.exportObject(obj, null);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
	in.defaultReadObject();
	reexport();
    }
    
    private void reexport()  throws RemoteException
    {
	if (ref != null) {
	    ((UnicastServerRef)ref).exportObject(this, null);
	} else {
	    exportObject((Remote)this);
	}
    }
}
