package ibis.satin.impl;

import ibis.satin.so.SharedObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public abstract class SOInvocationRecord implements java.io.Serializable {

    public String objectId;

    public SOInvocationRecord(String objectId) {
	this.objectId = objectId;
    }

    public abstract void invoke(SharedObject object);

    protected Object cloneObject(Object object) {
	Object copiedObject = null;
	
	try {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ObjectOutputStream oos = new ObjectOutputStream(baos);
	    oos.writeObject(object);
	    byte buf[] = baos.toByteArray();
	    oos.close();
    
	    ByteArrayInputStream bais = new ByteArrayInputStream(buf);
	    ObjectInputStream ois = new ObjectInputStream(bais);
	    copiedObject = ois.readObject();
	    ois.close();
	} catch (IOException e) {
	    System.err.println("SATIN '" + Satin.getSatin().ident.name() 
			       + "': error while copying write method parameters: "
			       + e);
	} catch (ClassNotFoundException e) {
	    System.err.println("SATIN '" + Satin.getSatin().ident.name() 
			       + "': error while copying write method parameters: "
			       + e);
	}
    
	return copiedObject;
    }

}
