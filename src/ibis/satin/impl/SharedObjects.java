package ibis.satin.impl;

import java.util.*;
import java.io.*;

import ibis.ipl.*;
import ibis.satin.so.*;
import ibis.util.messagecombining.*;

public abstract class SharedObjects extends TupleSpace implements Protocol {

    /*@todo: rethink the way objects are shipped, both at the beginning of the
    computation and in case of inconsistencies; instead of waiting for some time
    and only then shipping, start shipping immediately and if the object arrives 
    in the meantime, cancel the request*/
    
    HashMap sharedObjects = new HashMap();
    boolean gotObject = false;
    SharedObject object = null;

    Vector soInvocationsToSend = new Vector();
    long soInvocationsToSendTimer = -1;
    int writtenInvocations = 0;

    Map soSendPorts = new Hashtable();

    final static int WAIT_FOR_UPDATES_TIME = 5000;

    final static int WAIT_FOR_OBJECT_TIME = 8000;

    final static int LOOKUP_WAIT_TIME = 5000;

    public void broadcastSharedObject(SharedObject object) {
	WriteMessage w = null;
	long size = 0;
	if (SO_TIMING) {
	    handleSOTransferTimer.start();
	}
	try {
	    if (soInvocationsDelay > 0) {
		//do message combining
		w = soMessageCombiner.newMessage();
	    } else {
		w = soSendPort.newMessage();
	    }

	    if (SO_TIMING) {
		soSerializationTimer.start();
	    }
	    w.writeByte(SO_TRANSFER);
	    w.writeObject(object);
	    size = w.finish();
	    if (SO_TIMING) {
		soSerializationTimer.stop();
	    }
	    if (soInvocationsDelay > 0) {
		soMessageCombiner.sendAccumulatedMessages();
	    }
		
	} catch (IOException e) {
	    System.err.println("SATIN '" + ident.name()
			       + "': unable to broadcast a shared object: "
			       + e);
	}

	/*Iterator iter = soSendPorts.values().iterator();
	while (iter.hasNext()) {
	    try {
		if (soInvocationsDelay > 0) {
		    MessageCombiner mc = (MessageCombiner) iter.next();
		    w = mc.newMessage();
		    if (soInvocationsDelayTimer == -1) {
			soInvocationsDelayTimer = System.currentTimeMillis();
		    }
		} else {
		    SendPort send = (SendPort) iter.next();
		    w = send.newMessage();
		}
		if (SO_TIMING) {
		    soSerializationTimer.start();
		}		
		w.writeByte(SO_TRANSFER);
		w.writeObject(object);
		size = w.finish();
		if (SO_TIMING) {
		    soSerializationTimer.stop();
		}
	    } catch (IOException e) {
		System.err.println("SATIN '" + ident.name()
				   + "': unable to send a shared object: "
				   + e);
	    }
	    }*/


	//stats
	soTransfers += soSendPort.connectedTo().length;
	//soTransfers += soSendPorts.size();
	soTransfersBytes += size;
	if (SO_TIMING) {
	    handleSOTransferTimer.stop();
	}
    }
    

    /** Add an object to the object table*/
    public void addObject(SharedObject object) {
	synchronized (this) {
	    sharedObjects.put(object.objectId, object);
	}
    }

    void sendAccumulatedSOInvocations() {
	long currTime = System.currentTimeMillis();
	if (soInvocationsDelayTimer > 0 &&
	    (currTime - soInvocationsDelayTimer) > soInvocationsDelay) {       
	    try {
		soMessageCombiner.sendAccumulatedMessages();
		//WriteMessage w = soSendPort.newMessage();
		//w.writeInt(soInvocationsToSend.size());
		//while(true) {
		//    if( soInvocationsToSend.size() == 0) {
		//	break;
		//    }
		//    SOInvocationRecord soir = 
		//	(SOInvocationRecord) soInvocationsToSend.remove(0);
		//    w.writeObject(soir);
		//}
		//long byteCount = w.finish();
		//soInvocationsBytes += byteCount;
		writtenInvocations = 0;
	    } catch (IOException e) {
		System.err.println("SATIN '" + ident.name()
				   + "': unable to broadcast shared object invocations "
				   + e);
	    }
	    /*Iterator iter = soSendPorts.values().iterator();
	    while (iter.hasNext()) {
		try {
		    MessageCombiner mc = (MessageCombiner) iter.next();
		    mc.sendAccumulatedMessages();
		} catch (IOException e) {
		System.err.println("SATIN '" + ident.name()
				   + "': unable to send shared object invocations "
				   + e);				   
		}
		}*/
	}
	soInvocationsDelayTimer = -1;
    }


    /** Broadcast an so invocation*/
    public void broadcastSOInvocation(SOInvocationRecord r) {

	long byteCount = 0;
	int numToSend;
	WriteMessage w = null;

	if (SO_TIMING) {
	    broadcastSOInvocationsTimer.start();
	}


	if (soSendPort.connectedTo().length > 0) {

	    try {
		if (soInvocationsDelay > 0) {
		    //do message combining 
		    w = soMessageCombiner.newMessage();
		    //soInvocationsToSend.add(r);
		    if (soInvocationsDelayTimer == -1) {
			soInvocationsDelayTimer = System.currentTimeMillis();
		    }
		    writtenInvocations++;
		} else {
		    w = soSendPort.newMessage();
		    //   w.writeInt(1);
		    //w.writeObject(r);
		    //byteCount = w.finish();
		}

		w.writeByte(SO_INVOCATION);
		w.writeObject(r);
		byteCount = w.finish();
		
	    } catch (IOException e) {
		System.err.println("SATIN '" + ident.name()
				   + "': unable to broadcast a shared object invocation: "
				   + e);
	    }
	}

	/*Iterator iter = soSendPorts.values().iterator();
	while (iter.hasNext()) {
	    try {
		if (soInvocationsDelay > 0) {
		    MessageCombiner mc = (MessageCombiner) iter.next();
		    w = mc.newMessage();
		    if (soInvocationsDelayTimer == -1) {
			soInvocationsDelayTimer = System.currentTimeMillis();
		    }
		} else {
		    SendPort send = (SendPort) iter.next();
		    w = send.newMessage();
		}
		
		w.writeByte(SO_INVOCATION);
		w.writeObject(r);
		byteCount = w.finish();
	    } catch (IOException e) {
		System.err.println("SATIN '" + ident.name()
				   + "': unable to send a shared object invocation: "
				   + e);
	    }
	    }*/

	//stats
	soInvocations++;
	soInvocationsBytes += byteCount;

	if (SO_TIMING) {
	    broadcastSOInvocationsTimer.stop();
	}

    }

    /** Execute all the so invocations stored in the
	so invocations list */
    void handleSOInvocations() {
	SharedObject so = null;
	SOInvocationRecord soir = null;
	String soid = null;	

	gotSOInvocations = false;
	while (true) {
	    if (SO_TIMING) {
		handleSOInvocationsTimer.start();
	    }
	    //	    synchronized (this) {		
		if (soInvocationList.size() == 0) {
		    if (SO_TIMING) {
			handleSOInvocationsTimer.stop();
		    }
		    return;
		}
		soir = (SOInvocationRecord) soInvocationList.remove(0);
		soid = soir.objectId;
		so = (SharedObject) sharedObjects.get(soid);

		if (so == null) {
		    if (SO_TIMING) {
			handleSOInvocationsTimer.stop();
		    }
		    return;
		}
		//invoke while holding a lock. 
		//otherwise object transfer requests can be handled
		//in the middle of a method invocation
		soir.invoke(so);	    
		//	    }
	    if (SO_TIMING) {
		handleSOInvocationsTimer.stop();
	    }
	}

    }

    /** Return a reference to a shared object */
    public SharedObject getSOReference(String objectId) {
	synchronized (this) {
	    SharedObject obj = (SharedObject) sharedObjects.get(objectId);
	    if (obj == null) {
		System.err.println("OOPS, object not found in getSOReference");
	    }
	    return obj;
	}
    }

    /** Check if the given shared object is in the table,
	if not, ship it from source */
    public void setSOReference(String objectId, IbisIdentifier source) 
    throws SOReferenceSourceCrashedException {
	SharedObject obj = null;
	
	synchronized (this) {
	    obj = (SharedObject) sharedObjects.get(objectId);
	}
	if (obj == null) {
	    if (!initialNode) {
		shipObject(objectId, source);
	    } else {
		//just wait, object has been broadcast
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < WAIT_FOR_OBJECT_TIME) {
		    handleDelayedMessages();
		    synchronized (this) {
			obj = (SharedObject) sharedObjects.get(objectId);
		    }
		    if (obj != null) {
			break;
		    }
		}
		if (obj == null) {
		    shipObject(objectId, source);
		}
	    }
	}
	    
    }

    /** Receive a shared object from another node 
	(called by the MessageHandler */
    public void receiveObject(SharedObject obj) {
	synchronized (this) {
	    gotObject = true;
	    object = obj;
	}
    }

    /** Add a shared object invocation record to the
	so invocation record list; the invocation
	will be executed later */
    public void addSOInvocation(SOInvocationRecord soir) {
	synchronized (this) {
	    soInvocationList.add(soir);
	    gotSOInvocations = true;
	}
    }

    /** Add a new connection to the soSendPort */
    public void addSOConnection(IbisIdentifier id) {
        if (ASSERTS) {
            Satin.assertLocked(this);
        }

	try {
	    //create a receive port for this guy
	    SOInvocationHandler soInvocationHandler = new SOInvocationHandler(Satin.this_satin);
	    ReceivePort rec = soPortType.createReceivePort("satin so receive port on " 
							   + ident.name()
							   + " for " + id.name(),
							   soInvocationHandler);
	    if (soInvocationsDelay > 0) {
		    StaticProperties s = new StaticProperties();
		    s.add("serialization", "ibis");  
		    soInvocationHandler.setMessageSplitter(new MessageSplitter(s, rec));
	    }
	    rec.enableConnections();
	    rec.enableUpcalls();
	    
	    //create a send port for this guy
	    /*SendPort send = soPortType.createSendPort("satin so send port on "
						      + ident.name()
						      + " for " + id.name());
	    if (soInvocationsDelay > 0) {
		StaticProperties s = new StaticProperties();
		s.add("serialization", "ibis");
		MessageCombiner mc = new MessageCombiner(s, send);
		soSendPorts.put(id, mc);
	    } else {
		soSendPorts.put(id, send);
		}*/
	    //lookup his receive port
            ReceivePortIdentifier r = null;
	    r = lookup_wait("satin so receive port on "
			    + id.name() + " for " + ident.name(), LOOKUP_WAIT_TIME);
	    /*	    r = lookup_wait("satin so receive port on " + id.name(),
		    LOOKUP_WAIT_TIME);*/
	    //and connect
            if (!Satin.connect(soSendPort/*send*/, r, connectTimeout)) {
                System.err.println("SATN '" + ident.name()
                        + "': unable to connect to so receive port ");
            }
	} catch (IOException e) {
            System.err.println("SATN '" + ident.name()
                    + "': unable to connect to so receive port");
            e.printStackTrace();
	}
    } 

    /** Execute the guard of the invocation record r,
	wait for updates, if necessary,
	ship objects if necessary */
    void executeGuard(InvocationRecord r) 
	throws SOReferenceSourceCrashedException {
	boolean satisfied;
	long startTime;
	
	satisfied = r.guard();
	if (!satisfied) {
	    System.err.println("guard not satisfied, waiting for updates..");
	    startTime = System.currentTimeMillis();
	    while (System.currentTimeMillis() - startTime < WAIT_FOR_UPDATES_TIME
		   && !satisfied) {
		handleDelayedMessages();
		satisfied = r.guard();
	    }
	    if (!satisfied) {
		//try to ship the object from the owner of the job
		System.err.println("guard not satisfied, trying to ship shared objects..");
		Vector objRefs = r.getSOReferences();
		if (objRefs == null) {
		    System.err.println("oops, so references vector null!");
		    System.exit(1);
		}
		while (!objRefs.isEmpty()) {
		    String ref = (String) objRefs.remove(0);
		    shipObject(ref, r.owner);
		}
		System.err.println("objects shipped, checking again..");
		if (!r.guard()) {
		    System.err.println("SATIN '" + ident.name() +"':"
				       + " panic! inconsistent after shipping objects");
		    System.exit(1);
		}
		System.err.println("ok");
		    
	    }
	}
	
    }
    

    /** Ship a shared object from another node */
    private void shipObject(String objectId, IbisIdentifier source)
	throws SOReferenceSourceCrashedException {
	//request the shared object from the source
	if (SO_TIMING) {
	    soTransferTimer.start();
	}
	try {
	    //System.err.println("sending so request to " + source + ", objectId: " + objectId);
	    currentVictim = source;
	    SendPort s = getReplyPortWait(source);
	    WriteMessage w = s.newMessage();
	    w.writeByte(SO_REQUEST);
	    w.writeString(objectId);
	    w.finish();
	} catch (IOException e) {
	    //hm we've got a problem here
	    //push the job somewhere else?
	    System.err.println("SATIN '" + ident.name() + "': could not "
			       + "read shared object, exiting");
	    System.exit(1);
	}
	//wait for the reply
	while (true) {
	    //	    handleDelayedMessages();
	    synchronized (this) {
		if (gotObject) {
		    gotObject = false;
		    currentVictimCrashed = false;
		    break;
		}
		if (currentVictimCrashed) {
		    currentVictimCrashed = false;
		    break;
		}
	    }
	}
	if (SO_TIMING) {
	    soTransferTimer.stop();
	}
	if (object == null) {
	    //the source has crashed, abort the job
	    throw new SOReferenceSourceCrashedException();
	}
	synchronized (this) {
	    sharedObjects.put(object.objectId, object);
	}
	object = null;	
	System.err.println("SATIN '" + ident.name() + "': received shared object from " + source);
	handleDelayedMessages();
    }
}    
