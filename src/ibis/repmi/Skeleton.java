package ibis.repmi;

import java.io.IOException;

import ibis.ipl.ReadMessage;

// This is a base class for generated group stubs

public abstract class Skeleton { 
       	
	public ReplicatedObject destination;

	protected Skeleton() { 
		// does this do anything ?
	} 

	protected void init(ReplicatedObject destination) { 
		this.destination = destination;
	}  

	public abstract void handleMessage(ReadMessage r) throws IOException;	
}


