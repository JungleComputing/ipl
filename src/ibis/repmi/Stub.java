package manta.repmi;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;

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
	
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException  {
		objectID = s.readInt();
		localSkeleton = RTS.findSkeleton(objectID);
	}

	/* THIS IS THE MANTA.IO PART */
	public Stub(MantaInputStream mantainputstream) throws IOException, ClassNotFoundException {
		mantainputstream.addObjectToCycleCheck(this);
		objectID = mantainputstream.readInt();
		localSkeleton = RTS.findSkeleton(objectID);
	}
	
	public void generated_WriteObject(MantaOutputStream mantaoutputstream) throws IOException {
		mantaoutputstream.writeInt(objectID);
	}
	
	public void generated_ReadObject(MantaInputStream mantainputstream) throws IOException, ClassNotFoundException {
		objectID = mantainputstream.readInt();
		localSkeleton = RTS.findSkeleton(objectID);
	}
}



