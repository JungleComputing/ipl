package ibis.rmi.server;

import ibis.rmi.RemoteException;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;

public class UnicastRef implements RemoteRef, java.io.Serializable
{
    public String GUID = null;


    public UnicastRef() 
    {
    }
    
    public UnicastRef(String GUID) {
	this.GUID = GUID;
    }
    

    public RemoteCall newCall(RemoteObject obj, Operation[] ops, int opnum,
			      long hash)
	throws RemoteException
    {
	// Never called
	return null;
    }

    public void invoke(RemoteCall call) throws Exception
    {
	// Never called
    }

    public void done(RemoteCall call) throws RemoteException {
	// do nothing.
    }

    public String getRefClass(ObjectOutput out)
    {
	return "UnicastRef";
    }

    public void writeExternal(ObjectOutput out) throws IOException 
    {
	out.writeObject(GUID);
    }

    public void readExternal(ObjectInput in)
	throws IOException, ClassNotFoundException
    {
	GUID = (String)in.readObject();
    }
    
    public String remoteToString() {
	return "RemoteReference" + "@" + Integer.toHexString(System.identityHashCode(this));
    }

    Object invoke(RemoteStub stub , Method m, Object []param , long c) {
	return null;
    }

    public int remoteHashCode() {
	//return skeletonPortId.hashCode();
	return GUID.hashCode();
    }

    public boolean remoteEquals(RemoteRef sub) {
	if (sub instanceof UnicastRef) {
	    return GUID.equals(((UnicastRef)sub).GUID);
	} else {
	    return false;
	}
    }
}
