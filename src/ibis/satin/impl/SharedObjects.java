package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.StaticProperties;
import ibis.ipl.WriteMessage;
import ibis.satin.SharedObject;
import ibis.util.messagecombining.MessageSplitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

public abstract class SharedObjects extends TupleSpace implements Protocol {

    /*@todo: rethink the way objects are shipped, both at the beginning of the
     computation and in case of inconsistencies; instead of waiting for some time
     and only then shipping, start shipping immediately and if the object arrives 
     in the meantime, cancel the request*/

    HashMap sharedObjects = new HashMap();

    private HashMap ports = new HashMap();

    boolean gotObject = false;

    SharedObject object = null;

    ArrayList toConnect = new ArrayList();

    Vector soInvocationsToSend = new Vector();

    long soInvocationsToSendTimer = -1;

    int writtenInvocations = 0;

    Map soSendPorts = new Hashtable();

    final static int WAIT_FOR_UPDATES_TIME = 5000;

    final static int WAIT_FOR_OBJECT_TIME = 8000;

    final static int LOOKUP_WAIT_TIME = 10000;

/** This basicaly is optional, if nodes don't have the object, they will retrieve it.
    However, one broadcast is more efficient (serialization is done only once).
*/
    public void broadcastSharedObject(SharedObject object) {

         WriteMessage w = null;
         long size = 0;

         if(!SHARED_OBJECTS) {
         System.err.println("Shared objects are disabled.");
         System.exit(1);
         }
         
         if (SCALABLE) {
         doConnectSendPort();
         }

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
         + "': unable to broadcast a shared object: " + e);
         }

         //         Iterator iter = soSendPorts.values().iterator();
         //          while (iter.hasNext()) {
         //          try {
         //          if (soInvocationsDelay > 0) {
         //          MessageCombiner mc = (MessageCombiner) iter.next();
         //          w = mc.newMessage();
         //          if (soInvocationsDelayTimer == -1) {
         //          soInvocationsDelayTimer = System.currentTimeMillis();
         //          }
         //          } else {
         //          SendPort send = (SendPort) iter.next();
         //          w = send.newMessage();
         //          }
         //          if (SO_TIMING) {
         //          soSerializationTimer.start();
         //          }		
         //          w.writeByte(SO_TRANSFER);
         //          w.writeObject(object);
         //          size = w.finish();
         //          if (SO_TIMING) {
         //          soSerializationTimer.stop();
         //          }
         //          } catch (IOException e) {
         //          System.err.println("SATIN '" + ident.name()
         //          + "': unable to send a shared object: "
         //          + e);
         //          }
         //          }

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
        long elapsed = currTime - soInvocationsDelayTimer;
        if (soInvocationsDelayTimer > 0
            && (elapsed > soInvocationsDelay || soCurrTotalMessageSize > soMaxMessageSize)) {
            try {
                if (SO_TIMING) {
                    broadcastSOInvocationsTimer.start();
                }

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
            } catch (IOException e) {
                System.err.println("SATIN '" + ident.name()
                    + "': unable to broadcast shared object invocations " + e);
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
            soRealMessageCount++;
            writtenInvocations = 0;
            soCurrTotalMessageSize = 0;
            soInvocationsDelayTimer = -1; // @@@ AARG, this line was outside the if, that can't be correct, right? Rob

            if (SO_TIMING) {
                broadcastSOInvocationsTimer.stop();
            }
        }
    }

    /** Broadcast an so invocation*/
    public void broadcastSOInvocation(SOInvocationRecord r) {

        long byteCount = 0;
        //	int numToSend;
        WriteMessage w = null;

        if (!SHARED_OBJECTS) {
            System.err.println("Shared objects are disabled.");
            System.exit(1);
        }

        if (SO_TIMING) {
            broadcastSOInvocationsTimer.start();
        }

        if (SCALABLE) {
            doConnectSendPort();
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

                if (soInvocationsDelay > 0) {
                    soCurrTotalMessageSize += byteCount;
                } else {
                    soRealMessageCount++;
                }
            } catch (IOException e) {
                System.err
                    .println("SATIN '" + ident.name()
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

        // @@@ I added this code, shouldn't we try to send immediately if needed?
        // We might not reach a safe point for a considerable time
        if (soInvocationsDelay > 0) {
            sendAccumulatedSOInvocations();
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
            notifyAll();
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

    /**
     * Creates SO receive ports for new Satin instances.
     * Do this first, to make them available as soon as possible.
     */
    public void createSoReceivePorts(IbisIdentifier[] joiners) {
        for (int i = 0; i < joiners.length; i++) {
            //create a receive port for this guy
            try {
                SOInvocationHandler soInvocationHandler = new SOInvocationHandler(
                    Satin.this_satin);
                ReceivePort rec = soPortType.createReceivePort(
                    "satin so receive port on " + ident.name() + " for "
                        + joiners[i].name(), soInvocationHandler);
                if (soInvocationsDelay > 0) {
                    StaticProperties s = new StaticProperties();
                    s.add("serialization", "ibis");
                    soInvocationHandler.setMessageSplitter(new MessageSplitter(
                        s, rec));
                }
                rec.enableConnections();
                rec.enableUpcalls();
            } catch (Exception e) {
                System.err.println("SATIN '" + ident.name()
                    + "': unable to create so receive port");
                e.printStackTrace();
            }
        }
    }

    private synchronized void doConnectSendPort() {

        for (int i = 0; i < toConnect.size(); i++) {
            IbisIdentifier id = (IbisIdentifier) toConnect.get(i);
            ReceivePortIdentifier r = lookup_wait("satin so receive port on "
                + id.name() + " for " + ident.name(), LOOKUP_WAIT_TIME);
            //and connect
            if (r == null
                || !Satin.connect(soSendPort/*send*/, r, connectTimeout)) {
                System.err.println("SATN '" + ident.name()
                    + "': unable to connect to so receive port ");
            } else {
                ports.put(id, r);
            }
        }
        toConnect.clear();
    }

    /** Add a new connection to the soSendPort */
    public void addSOConnection(IbisIdentifier id) {
        if (ASSERTS) {
            Satin.assertLocked(this);
        }

        if (SCALABLE) {
            toConnect.add(id);
            return;
        }

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
        ReceivePortIdentifier r = lookup_wait("satin so receive port on "
            + id.name() + " for " + ident.name(), LOOKUP_WAIT_TIME);
        /*	    r = lookup_wait("satin so receive port on " + id.name(),
         LOOKUP_WAIT_TIME);*/
        //and connect
        if (r == null || !Satin.connect(soSendPort/*send*/, r, connectTimeout)) {
            System.err.println("SATN '" + ident.name()
                + "': unable to connect to so receive port ");
        } else {
            ports.put(id, r);
        }
    }

    /** Remove a new connection to the soSendPort */
    public void removeSOConnection(IbisIdentifier id) {
        if (ASSERTS) {
            Satin.assertLocked(this);
        }

        ReceivePortIdentifier r = (ReceivePortIdentifier) ports.remove(id);
        //and disconnect
        if (r != null) {
            Satin.disconnect(soSendPort, r);
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
            if (soLogger.isInfoEnabled()) {
                soLogger.info("SATIN '" + ident.name() + "': "
                    + "guard not satisfied, waiting for updates..");
            }
            startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < WAIT_FOR_UPDATES_TIME
                && !satisfied) {
                handleDelayedMessages();
                satisfied = r.guard();
            }
            if (!satisfied) {
                //try to ship the object from the owner of the job
                if (soLogger.isInfoEnabled()) {
                    soLogger
                        .info("SATIN '"
                            + ident.name()
                            + "': "
                            + "guard not satisfied, trying to ship shared objects ...");
                }
                Vector objRefs = r.getSOReferences();
                if (ASSERTS && objRefs == null) {
                    soLogger.fatal("SATIN '" + ident.name() + "': "
                        + "oops, so references vector null!");
                    System.exit(1); // Failed assert
                }
                while (!objRefs.isEmpty()) {
                    String ref = (String) objRefs.remove(0);
                    shipObject(ref, r.owner);
                }
                if (ASSERTS) {
                    if (soLogger.isInfoEnabled()) {
                        soLogger.info("SATIN '" + ident.name() + "': "
                            + "objects shipped, checking again..");
                    }
                    if (!r.guard()) {
                        soLogger.fatal("SATIN '" + ident.name() + "':"
                            + " panic! inconsistent after shipping objects");
                        System.exit(1); // Failed assert
                    }
                }
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
            Victim v = getVictimWait(source);
            WriteMessage w = v.newMessage();
            w.writeByte(SO_REQUEST);
            w.writeString(objectId);
            w.finish();
        } catch (IOException e) {
            //hm we've got a problem here
            //push the job somewhere else?
            soLogger.error("SATIN '" + ident.name() + "': could not "
                + "write shared-object request", e);
            if (SO_TIMING) {
                soTransferTimer.stop();
            }
            throw new SOReferenceSourceCrashedException();
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

                try {
                    wait();
                } catch (Exception e) {
                    // ignore
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
        soLogger.info("SATIN '" + ident.name()
            + "': received shared object from " + source);
        handleDelayedMessages();
    }

    void addToSORequestList(IbisIdentifier requester, String objID) {
        if (ASSERTS) {
            assertLocked(this);
        }
        SORequestList.add(requester, objID);
        gotSORequests = true;
    }

    void handleSORequests() {
        gotSORequests = false;
        WriteMessage wm;
        long size;
        IbisIdentifier origin;
        String objid;

        while (true) {

            synchronized (this) {
                if (SORequestList.getCount() == 0) return;
                origin = SORequestList.getRequester(0);
                objid = SORequestList.getobjID(0);
                SORequestList.removeIndex(0);
            }

            if (SO_TIMING) {
                handleSOTransferTimer = createTimer();
                handleSOTransferTimer.start();
            }

            SharedObject so = getSOReference(objid);
            //System.err.println("got object");
            Victim v = getVictimWait(origin);
            //System.err.println("got reply port");

            if (ASSERTS && so == null) {
                soLogger.fatal("SATIN '" + ident.name()
                    + "': EEEK, requested shared object: " + objid
                    + " not found! Exiting..");
                System.exit(1); // Failed assertion
            }

            if (SO_TIMING) {
                soSerializationTimer = createTimer();
                soSerializationTimer.start();
            }

            //we need to hold the lock while writing the object
            //otherwise some update might change the state of the object
            //what's worse: the update might be executed only partially
            //before the object is sent
            try {
                wm = v.newMessage();
                wm.writeByte(SO_TRANSFER);
                wm.writeObject(so);
                size = wm.finish();
                //      System.err.println("sent object");
                //stats
                soTransfers++;
                soTransfersBytes += size;
                if (SO_TIMING) {
                    handleSOTransferTimer.stop();
                    handleSOTransferTimer.add(handleSOTransferTimer);
                    soSerializationTimer.stop();
                    soSerializationTimer.add(soSerializationTimer);
                }
            } catch (IOException e) {
                soLogger.error("SATIN '" + ident.name()
                    + "': got exception while sending" + " shared object", e);
            }
        }
    }
}
