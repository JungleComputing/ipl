package ibis.rmi.impl;

import ibis.rmi.server.RemoteRef;
import ibis.rmi.server.RemoteStub;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;

public class UnicastRef implements RemoteRef, java.io.Serializable
{
    public String GUID = null;


    public UnicastRef() {
        // nothing here
    }
    
    public UnicastRef(String GUID) {
	this.GUID = GUID;
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
	}
	return false;
    }
}
