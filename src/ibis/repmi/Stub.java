package ibis.repmi;

import ibis.io.IbisSerializationInputStream;
import ibis.io.IbisSerializationOutputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

// This is a base class for generated group stubs

public class Stub implements java.io.Serializable, ibis.io.Serializable { 

    // all set by the RTS.
    protected int objectID;
    protected transient Skeleton localSkeleton;

    public Stub() { 
    } 

    protected void init(int objectID, Skeleton localSkeleton) { 		
	this.objectID      = objectID;
	this.localSkeleton = localSkeleton;
    } 

    /* THIS IS THE JAVA.IO PART */
    private void writeObject(ObjectOutputStream s) throws IOException {
	s.writeInt(objectID);
    }

    private void readObject(ObjectInputStream s) throws IOException {
	objectID = s.readInt();
	localSkeleton = RTS.findSkeleton(objectID);
    }

    public Stub(IbisSerializationInputStream mantainputstream) throws IOException {
	mantainputstream.addObjectToCycleCheck(this);
	objectID = mantainputstream.readInt();
	localSkeleton = RTS.findSkeleton(objectID);
    }

    public void generated_WriteObject(IbisSerializationOutputStream mantaoutputstream) throws IOException {
	mantaoutputstream.writeInt(objectID);
    }

    public void generated_DefaultWriteObject(IbisSerializationOutputStream mantaoutputstream, int dummy) throws IOException {
	mantaoutputstream.writeInt(objectID);
    }

    public void generated_ReadObject(IbisSerializationInputStream mantainputstream) throws IOException {
	objectID = mantainputstream.readInt();
	localSkeleton = RTS.findSkeleton(objectID);
    }

    public void generated_DefaultReadObject(IbisSerializationInputStream mantainputstream, int dummy) throws IOException {
	objectID = mantainputstream.readInt();
	localSkeleton = RTS.findSkeleton(objectID);
    }
}



