package ibis.rmi.server;

import ibis.rmi.Remote;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.rmi.MarshalException;
import java.rmi.UnmarshalException;
import java.io.IOException;


public abstract class RemoteObject implements Remote, Serializable
{
    public static final long serialVersionUID = -3215090123894869218l;
    transient public RemoteRef ref;

    protected RemoteObject() {
	this(null);
    }
    
    protected RemoteObject(RemoteRef newref) {
	ref = newref;
    }

    public int hashCode() {
	int hashcode = (ref == null) ? super.hashCode() : ref.remoteHashCode();
	return hashcode;
    }

    public boolean equals(Object obj) {
	if (obj instanceof RemoteObject) {
	    if (ref == null) {
		return obj == this;
	    } else {
		return ref.remoteEquals(((RemoteObject)obj).ref);
	    }
	} else if (obj != null) {
	    return obj.equals(this);
	} else {
	    return false;
	}
    }

    public String toString()
    {
	String classname = this.getClass().getName();
	String hc = "@" + Integer.toHexString(System.identityHashCode(this));
	return (ref == null) ? classname + hc :
	    classname + hc + "[" +ref.remoteToString() + "]";
    }


    private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException
    {
//	System.out.println("writeObject: " + this);
	if (ref == null) {
	    throw new MarshalException("no ref to serialize");
	} else {
	    out.writeUTF(ref.getRefClass(out));
	    ref.writeExternal(out);
	}

    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
	String name = RemoteRef.packagePrefix + "." + in.readUTF();
	try {
	    Class cls = Class.forName(name);
	    ref = (RemoteRef)cls.newInstance();
	    ref.readExternal(in);
	} catch (InstantiationException e) {
	    throw new UnmarshalException("failed to instantiate ref");
	} catch (IllegalAccessException e) {
	    throw new UnmarshalException("failed to create ref");
	}
//	System.out.println("readObject: " + this);
    }

}
