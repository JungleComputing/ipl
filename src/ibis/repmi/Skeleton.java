package ibis.repmi;

import ibis.ipl.ReadMessage;
import ibis.ipl.IbisIOException;

// This is a base class for generated group stubs

public abstract class Skeleton { 
       	
	public ReplicatedObject destination;

	protected Skeleton() { 
		// does this do anything ?
	} 

	protected void init(ReplicatedObject destination) { 
		this.destination = destination;
	}  

	public abstract void handleMessage(ReadMessage r) throws IbisIOException;	
}


