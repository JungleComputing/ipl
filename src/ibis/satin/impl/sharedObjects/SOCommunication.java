// @@@ msg combining AND lrmc at the same time is not supported
// @@@ TODO? LRMC echt een ring maken, zodat je weet dat de mcast klaar is ->
// handig als een node om updates/object vraagt

/*
 * Created on Apr 26, 2006 by rob
 */
package ibis.satin.impl.sharedObjects;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.satin.SharedObject;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.communication.Communication;
import ibis.satin.impl.communication.Protocol;
import ibis.satin.impl.loadBalancing.Victim;
import ibis.satin.impl.spawnSync.InvocationRecord;
import ibis.util.DeepCopy;
import ibis.util.Timer;
import ibis.util.TypedProperties;
import ibis.util.messagecombining.MessageCombiner;
import ibis.util.messagecombining.MessageSplitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import mcast.object.ObjectMulticaster;
import mcast.object.SendDoneUpcaller;

class OmcInfo implements Config {
    static final class DataObject {
        Timer timer;
        long size;
        boolean done;
    }
    
    HashMap<Integer, DataObject> map = new HashMap<Integer, DataObject>();

    Timer total = Timer.createTimer();

    Satin s;

    public OmcInfo(Satin s) {
        this.s = s;
    }

    synchronized void registerSend(int id, long size) {
        DataObject d = new DataObject();
        d.timer = Timer.createTimer();
        d.size = size;
        map.put(id, d);
        d.timer.start();
    }

    public synchronized void sendDone(int id) {
        DataObject d = map.get(id);
        if (d == null) {
            soBcastLogger.info("SATIN '" + s.ident
                + "': got upcall for unknow id: " + id);
            return;
        }
        d.timer.stop();
        d.done = true;
        total.add(d.timer);
        notifyAll();
        
        soBcastLogger.info("SATIN '" + s.ident + "': broadcast " + id
            + " took " + d.timer.totalTime() + " " + ((d.size / d.timer.totalTimeVal()) / 1024 * 1024) + " MB/s");
    }

    public synchronized void waitForCompletion(int id) {
        DataObject d = map.remove(id);
        if (d == null) {
            soBcastLogger.info("SATIN '" + s.ident
                + "': wait for unknow id: " + id);
            return;
        }

        while(!d.done) {
            try {
                wait();
            } catch (Exception e) {
                // ignore
            }
        }
    }
    
    void end() {
        soBcastLogger.info("SATIN '" + s.ident
            + "': total broadcast time was: " + total.totalTime());
    }
}

final class SOCommunication implements Config, Protocol, SendDoneUpcaller {
    private static final boolean ASYNC_SO_BCAST = false;

    private final static int WAIT_FOR_UPDATES_TIME = 60000;

    public static final boolean DISABLE_SO_BCAST = false;
    
    private Satin s;

    /** the current size of the accumulated so messages */
    private long soCurrTotalMessageSize = 0;

    private long soInvocationsDelayTimer = -1;

    /** used to broadcast shared object invocations */
    private SendPort soSendPort;

    private PortType soPortType;

    /** used to do message combining on soSendPort */
    private MessageCombiner soMessageCombiner;

    /** a list of ibis identifiers that we still need to connect to */
    private ArrayList<IbisIdentifier> toConnect =
            new ArrayList<IbisIdentifier>();

    private HashMap<IbisIdentifier, ReceivePortIdentifier> ports =
            new HashMap<IbisIdentifier, ReceivePortIdentifier>();

    private ObjectMulticaster omc;

    private SharedObject sharedObject = null;

    private boolean receivedNack = false;

    private OmcInfo omcInfo;

    protected SOCommunication(Satin s) {
        this.s = s;
    }

    protected void init() {
        if(DISABLE_SO_BCAST) return;
    
        if (LABEL_ROUTING_MCAST) {
            try {
                omc = new ObjectMulticaster(s.comm.ibis,
                            true /* efficient multi-cluster */, false,
                            "satinSO", this);
                omcInfo = new OmcInfo(s);
            } catch (Exception e) {
                System.err.println("cannot create OMC: " + e);
                e.printStackTrace();
                System.exit(1);
            }

            new SOInvocationReceiver(s, omc).start();
        } else {
            try {
                soPortType = createSOPortType();

                // Create a multicast port to bcast shared object invocations.
                // Connections are established later.
                soSendPort =
                        s.comm.ibis.createSendPort(soPortType,
                                "satin so port on " + s.ident);

                if (SO_MAX_INVOCATION_DELAY > 0) {
                    TypedProperties props = new TypedProperties();
                    props.setProperty("ibis.serialization", "ibis");
                    soMessageCombiner = new MessageCombiner(props, soSendPort);
                }
            } catch (Exception e) {
                commLogger.fatal("SATIN '" + s.ident
                    + "': Could not start ibis: " + e, e);
                System.exit(1); // Could not start ibis
            }
        }
    }

    private PortType createSOPortType() throws IOException {
        return new PortType(
                PortType.CONNECTION_ONE_TO_MANY, PortType.CONNECTION_UPCALLS,
                PortType.CONNECTION_DOWNCALLS, PortType.RECEIVE_EXPLICIT, PortType.RECEIVE_AUTO_UPCALLS,
                PortType.SERIALIZATION_OBJECT);
    }

    /**
     * Creates SO receive ports for new Satin instances. Do this first, to make
     * them available as soon as possible.
     */
    protected void handleJoins(IbisIdentifier[] joiners) {
        if(DISABLE_SO_BCAST) return;
    
        // lrmc uses its own ports
        if (LABEL_ROUTING_MCAST) {
            for (int i = 0; i < joiners.length; i++) {
                omc.addIbis(joiners[i]);
            }

            // Set the destination for the multicast.
            // The victimtable does not contain the new joiners yet.
            IbisIdentifier[] victims;
            synchronized (s) {
                victims = s.victims.getIbises();
            }
            HashSet<IbisIdentifier> destinations = new HashSet<IbisIdentifier>();
            for (IbisIdentifier id : victims) {
                destinations.add(id);
            }
            for (IbisIdentifier id : joiners) {
                destinations.add(id);
            }
            omc.setDestination(destinations.toArray(new IbisIdentifier[destinations.size()]));
            return;
        }

        for (int i = 0; i < joiners.length; i++) {
            // create a receive port for this guy
            try {
                SOInvocationHandler soInvocationHandler =
                        new SOInvocationHandler(s);
                ReceivePort rec;

                rec =
                        s.comm.ibis.createReceivePort(soPortType,
                            "satin so receive port for " + joiners[i],
                            soInvocationHandler, s.ft
                                .getReceivePortConnectHandler(), null);

                if (SO_MAX_INVOCATION_DELAY > 0) {
                    TypedProperties s = new TypedProperties();
                    s.setProperty("ibis.serialization", "ibis");
                    soInvocationHandler.setMessageSplitter(new MessageSplitter(
                        s, rec));
                }
                rec.enableConnections();
                rec.enableMessageUpcalls();
            } catch (Exception e) {
                commLogger.fatal("SATIN '" + s.ident
                    + "': Could not start ibis: " + e, e);
                System.exit(1); // Could not start ibis
            }
        }

        /** Add new connections to the soSendPort */
        synchronized (s) {
            for (int i = 0; i < joiners.length; i++) {
                toConnect.add(joiners[i]);
            }
        }
    }

    protected void sendAccumulatedSOInvocations() {
        if (SO_MAX_INVOCATION_DELAY <= 0) return;

        long currTime = System.currentTimeMillis();
        long elapsed = currTime - soInvocationsDelayTimer;
        if (soInvocationsDelayTimer > 0
            && (elapsed > SO_MAX_INVOCATION_DELAY || soCurrTotalMessageSize > SO_MAX_MESSAGE_SIZE)) {
            try {
                s.stats.broadcastSOInvocationsTimer.start();

                soMessageCombiner.sendAccumulatedMessages();
            } catch (IOException e) {
                System.err.println("SATIN '" + s.ident
                    + "': unable to broadcast shared object invocations " + e);
            }

            s.stats.soRealMessageCount++;
            soCurrTotalMessageSize = 0;
            soInvocationsDelayTimer = -1;

            s.stats.broadcastSOInvocationsTimer.stop();
        }
    }

    protected void broadcastSOInvocation(SOInvocationRecord r) {
        if(DISABLE_SO_BCAST) return;
    
        if (LABEL_ROUTING_MCAST) {
            doBroadcastSOInvocationLRMC(r);
        } else {
            if (ASYNC_SO_BCAST) {
                // We have to make a copy of the object first, the caller might modify it.
                SOInvocationRecord copy =
                        (SOInvocationRecord) DeepCopy.deepCopy(r);
                new AsyncBcaster(this, copy).start();
            } else {
                doBroadcastSOInvocation(r);
            }
        }
    }

    public void sendDone(int id) {
        if (soLogger.isDebugEnabled()) {
            soLogger.debug("SATIN '" + s.ident + "': got ACK for send "
                + id);
        }

        omcInfo.sendDone(id);
    }

    /** Broadcast an so invocation */
    protected void doBroadcastSOInvocationLRMC(SOInvocationRecord r) {
        IbisIdentifier[] tmp;
        synchronized (s) {
            tmp = s.victims.getIbises();
            if (tmp.length == 0) return;
        }
        soBcastLogger.debug("SATIN '" + s.ident
            + "': broadcasting so invocation for: " + r.getObjectId());
        s.stats.broadcastSOInvocationsTimer.start();
        s.so.registerMulticast(s.so.getSOReference(r.getObjectId()), tmp);

        try {
            int id = omc.send(r);
            omcInfo.registerSend(id, omc.lastSize());
            omcInfo.waitForCompletion(id);
        } catch (Exception e) {
            soBcastLogger.warn("SOI mcast failed: " + e + " msg: " + e.getMessage());
        }

        s.stats.soInvocations++;
        s.stats.soRealMessageCount++;
        s.stats.soInvocationsBytes += omc.lastSize();
        s.stats.broadcastSOInvocationsTimer.stop();
    }

    /** Broadcast an so invocation */
    protected void doBroadcastSOInvocation(SOInvocationRecord r) {
        long byteCount = 0;
        WriteMessage w = null;

        s.stats.broadcastSOInvocationsTimer.start();

        connectSendPortToNewReceivers();

        IbisIdentifier[] tmp;
        synchronized (s) {
            tmp = s.victims.getIbises();
        }
        s.so.registerMulticast(s.so.getSOReference(r.getObjectId()), tmp);

        if (soSendPort != null && soSendPort.connectedTo().length > 0) {
            try {
                if (SO_MAX_INVOCATION_DELAY > 0) { // do message combining
                    w = soMessageCombiner.newMessage();
                    if (soInvocationsDelayTimer == -1) {
                        soInvocationsDelayTimer = System.currentTimeMillis();
                    }
                } else {
                    w = soSendPort.newMessage();
                }

                w.writeByte(SO_INVOCATION);
                w.writeObject(r);
                byteCount = w.finish();

            } catch (IOException e) {
                if (w != null) {
                    w.finish(e);
                }
                System.err
                    .println("SATIN '" + s.ident
                        + "': unable to broadcast a shared object invocation: "
                        + e);
            }
            if (SO_MAX_INVOCATION_DELAY > 0) {
                soCurrTotalMessageSize += byteCount;
            } else {
                s.stats.soRealMessageCount++;
            }
        }

        s.stats.soInvocations++;
        s.stats.soInvocationsBytes += byteCount;
        s.stats.broadcastSOInvocationsTimer.stop();

        // Try to send immediately if needed.
        // We might not reach a safe point for a considerable time.
        if (SO_MAX_INVOCATION_DELAY > 0) {
            sendAccumulatedSOInvocations();
        }
    }

    /**
     * This basicaly is optional, if nodes don't have the object, they will
     * retrieve it. However, one broadcast is more efficient (serialization is
     * done only once). We MUST use message combining here, we use the same receiveport
     * as the SO invocation messages. This is only called by exportObject. 
     */
    protected void broadcastSharedObject(SharedObject object) {
        if(DISABLE_SO_BCAST) return;
        
        if (LABEL_ROUTING_MCAST) {
            doBroadcastSharedObjectLRMC(object);
        } else {
            doBroadcastSharedObject(object);
        }
    }

    protected void doBroadcastSharedObject(SharedObject object) {
        WriteMessage w = null;
        long size = 0;

        s.stats.soBroadcastTransferTimer.start();

        connectSendPortToNewReceivers();

        if (soSendPort == null) {
            s.stats.soBroadcastTransferTimer.stop();
            return;
        }

        IbisIdentifier[] tmp;
        synchronized (s) {
            tmp = s.victims.getIbises();
        }
        s.so.registerMulticast(s.so.getSOReference(object.getObjectId()), tmp);

        try {
            if (SO_MAX_INVOCATION_DELAY > 0) {
                //do message combining
                w = soMessageCombiner.newMessage();
            } else {
                w = soSendPort.newMessage();
            }

            w.writeByte(SO_TRANSFER);
            s.stats.soBroadcastSerializationTimer.start();
            w.writeObject(object);
            s.stats.soBroadcastSerializationTimer.stop();
            size = w.finish();
            w = null;
            if (SO_MAX_INVOCATION_DELAY > 0) {
                soMessageCombiner.sendAccumulatedMessages();
            }
        } catch (IOException e) {
            if (w != null) {
                w.finish(e);
            }
            System.err.println("SATIN '" + s.ident
                + "': unable to broadcast a shared object: " + e);
        }

        s.stats.soBcasts++;
        s.stats.soBcastBytes += size;
        s.stats.soBroadcastTransferTimer.stop();
    }

    /** Broadcast an so invocation */
    protected void doBroadcastSharedObjectLRMC(SharedObject object) {
        IbisIdentifier[] tmp;
        synchronized (s) {
            tmp = s.victims.getIbises();
            if (tmp.length == 0) return;
        }

        soBcastLogger.info("SATIN '" + s.ident + "': broadcasting object: "
            + object.getObjectId());
        s.stats.soBroadcastTransferTimer.start();
        s.so.registerMulticast(object, tmp);

        try {
            s.stats.soBroadcastSerializationTimer.start();
            int id = omc.send(object);
            omcInfo.registerSend(id, omc.lastSize());            
            s.stats.soBroadcastSerializationTimer.stop();
            omcInfo.waitForCompletion(id);
        } catch (Exception e) {
            soBcastLogger.warn("SATIN '" + s.ident + "': SO mcast failed: " + e, e);
        }
        s.stats.soBcasts++;
        s.stats.soBcastBytes += omc.lastSize();
        s.stats.soBroadcastTransferTimer.stop();
    }

    /** Remove a connection to the soSendPort */
    protected void removeSOConnection(IbisIdentifier id) {
        Satin.assertLocked(s);
        ReceivePortIdentifier r = ports.remove(id);

        if (r != null) {
            Communication.disconnect(soSendPort, r);
        }
    }

    /** Fetch a shared object from another node.
     * If the Invocation record is null, any version is OK, we just test that we have a 
     * version of the object. If it is not null, we try to satisfy the guard of the 
     * invocation record. It might not be satisfied when this method returns, the
     * guard might depend on more than one shared object. */
    protected void fetchObject(String objectId, IbisIdentifier source,
            InvocationRecord r) throws SOReferenceSourceCrashedException {

        if(s.so.waitForObject(objectId, source, r, WAIT_FOR_UPDATES_TIME)) {
            return;
        }

        soLogger.debug("SATIN '" + s.ident
            + "': did not receive object in time, demanding it now");

        // haven't got it, demand it now.
        sendSORequest(objectId, source, true);

        boolean gotIt = waitForSOReply();
        if (gotIt) {
            soLogger.debug("SATIN '" + s.ident
                + "': received demanded object");
            return;
        }
        soLogger
            .fatal("SATIN '"
                + s.ident
                + "': internal error: did not receive shared object after I demanded it. ");
    }

    private void sendSORequest(String objectId, IbisIdentifier source,
            boolean demand) throws SOReferenceSourceCrashedException {
        // request the shared object from the source
        WriteMessage w = null;
        try {
            s.lb.setCurrentVictim(source);
            Victim v;
            synchronized (s) {
                v = s.victims.getVictim(source);
            }
            if (v == null) {
                // hm we've got a problem here
                // push the job somewhere else?
                soLogger.error("SATIN '" + s.ident + "': could not "
                    + "write shared-object request");
                throw new SOReferenceSourceCrashedException();
            }

            w = v.newMessage();
            if (demand) {
                w.writeByte(SO_DEMAND);
            } else {
                w.writeByte(SO_REQUEST);
            }
            w.writeString(objectId);
            v.finish(w);
        } catch (IOException e) {
            if (w != null) {
                w.finish(e);
            }
            // hm we've got a problem here
            // push the job somewhere else?
            soLogger.error("SATIN '" + s.ident + "': could not "
                + "write shared-object request", e);
            throw new SOReferenceSourceCrashedException();
        }
    }

    private boolean waitForSOReply() throws SOReferenceSourceCrashedException {
        // wait for the reply
        // there are three possibilities:
        // 1. we get the object back -> return true
        // 2. we get a nack back -> return false
        // 3. the source crashed -> exception
        while (true) {
            synchronized (s) {
                if (sharedObject != null) {
                    s.so.addObject(sharedObject);
                    sharedObject = null;
                    s.currentVictimCrashed = false;
                    soLogger.info("SATIN '" + s.ident
                        + "': received shared object");
                    return true;
                }
                if (s.currentVictimCrashed) {
                    s.currentVictimCrashed = false;
                    // the source has crashed, abort the job
                    soLogger.info("SATIN '" + s.ident
                        + "': source crashed while waiting for SO reply");
                    throw new SOReferenceSourceCrashedException();
                }
                if (receivedNack) {
                    receivedNack = false;
                    s.currentVictimCrashed = false;
                    soLogger.info("SATIN '" + s.ident
                        + "': received shared object NACK");
                    return false;
                }

                try {
                    s.wait();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    boolean broadcastInProgress(SharedObjectInfo info, IbisIdentifier dest) {
        if (System.currentTimeMillis() - info.lastBroadcastTime > WAIT_FOR_UPDATES_TIME) {
            return false;
        }

        for (int i = 0; i < info.destinations.length; i++) {
            if (info.destinations[i].equals(dest)) return true;
        }

        return false;
    }

    protected void handleSORequests() {
        WriteMessage wm = null;
        IbisIdentifier origin;
        String objid;
        boolean demand;

        while (true) {
            Victim v;
            synchronized (s) {
                if (s.so.SORequestList.getCount() == 0) {
                    s.so.gotSORequests = false;
                    return;
                }
                origin = s.so.SORequestList.getRequester(0);
                objid = s.so.SORequestList.getobjID(0);
                demand = s.so.SORequestList.isDemand(0);
                s.so.SORequestList.removeIndex(0);
                v = s.victims.getVictim(origin);
            }

            if (v == null) {
                soLogger.debug("SATIN '" + s.ident
                    + "': vicim crached in handleSORequest");
                continue; // node might have crashed
            }

            SharedObjectInfo info = s.so.getSOInfo(objid);
            if (ASSERTS && info == null) {
                soLogger.fatal("SATIN '" + s.ident
                    + "': EEEK, requested shared object: " + objid
                    + " not found! Exiting..");
                System.exit(1); // Failed assertion
            }

            if (!demand && broadcastInProgress(info, v.getIdent())) {
                soLogger.debug("SATIN '" + s.ident
                    + "': send NACK back in handleSORequest");
                // send NACK back
                try {
                    wm = v.newMessage();
                    wm.writeByte(SO_NACK);
                    v.finish(wm);
                } catch (IOException e) {
                    if (wm != null) {
                        wm.finish(e);
                    }
                    soLogger.error("SATIN '" + s.ident
                        + "': got exception while sending"
                        + " shared object NACK", e);
                }

                continue;
            }

            soLogger.debug("SATIN '" + s.ident
                + "': send object back in handleSORequest");
            sendObjectBack(v, info);
        }
    }

    private void sendObjectBack(Victim v, SharedObjectInfo info) {
        WriteMessage wm = null;
        long size;

        // No need to hold the lock while writing the object.
        // Updates cannot change the state of the object during the send, 
        // they are delayed until safe a point.
        s.stats.soTransferTimer.start();
        SharedObject so = info.sharedObject;
        try {
            wm = v.newMessage();
            wm.writeByte(SO_TRANSFER);
        } catch (IOException e) {
            s.stats.soTransferTimer.stop();
            if (wm != null) {
                wm.finish(e);
            }
            soLogger.error("SATIN '" + s.ident
                + "': got exception while sending" + " shared object", e);
            return;
        }

        s.stats.soSerializationTimer.start();
        try {
            wm.writeObject(so);
        } catch (IOException e) {
            s.stats.soSerializationTimer.stop();
            s.stats.soTransferTimer.stop();
            wm.finish(e);
            soLogger.error("SATIN '" + s.ident
                + "': got exception while sending" + " shared object", e);
            return;
        }
        s.stats.soSerializationTimer.stop();

        try {
            size = v.finish(wm);
        } catch (IOException e) {
            s.stats.soTransferTimer.stop();
            wm.finish(e);
            soLogger.error("SATIN '" + s.ident
                + "': got exception while sending" + " shared object", e);
            return;
        }

        s.stats.soTransfers++;
        s.stats.soTransfersBytes += size;
        s.stats.soTransferTimer.stop();
    }

    protected void handleSORequest(ReadMessage m, boolean demand) {
        String objid = null;
        IbisIdentifier origin = m.origin().ibisIdentifier();

        soLogger.info("SATIN '" + s.ident + "': got so request");

        try {
            objid = m.readString();
            // no need to finish the message. We don't do any communication
        } catch (IOException e) {
            soLogger.warn("SATIN '" + s.ident
                + "': got exception while reading" + " shared object request: "
                + e.getMessage());
        }

        synchronized (s) {
            s.so.addToSORequestList(origin, objid, demand);
        }
    }

    /**
     * Receive a shared object from another node (called by the MessageHandler
     */
    protected void handleSOTransfer(ReadMessage m) { // normal so transfer (not exportObject)
        SharedObject obj = null;

        s.stats.soDeserializationTimer.start();
        try {
            obj = (SharedObject) m.readObject();
        } catch (IOException e) {
            soLogger.error("SATIN '" + s.ident
                + "': got exception while reading" + " shared object", e);
        } catch (ClassNotFoundException e) {
            soLogger.error("SATIN '" + s.ident
                + "': got exception while reading" + " shared object", e);
        }
        s.stats.soDeserializationTimer.stop();

        // no need to finish the read message here. 
        // We don't block and don't do any communication
        synchronized (s) {
            sharedObject = obj;
            s.notifyAll();
        }
    }

    protected void handleSONack(ReadMessage m) {
        synchronized (s) {
            receivedNack = true;
            s.notifyAll();
        }
    }

    private void connectSOSendPort(IbisIdentifier ident) {
        ReceivePortIdentifier r =
                Communication.connect(soSendPort, ident,
                    "satin so receiveport for " + s.ident,
                    Satin.CONNECT_TIMEOUT);
        if (r != null) {
            synchronized (s) {
                ports.put(ident, r);
            }
        } else {
            soLogger.warn("SATIN '" + s.ident
                + "': unable to connect to SO receive port ");
            // We won't broadcast the object to this receiver.
            // This is not really a problem, it will get the object if it
            // needs it. But the node has probably crashed anyway.
            return;
        }
    }

    private void connectSendPortToNewReceivers() {
        IbisIdentifier[] tmp;
        synchronized (s) {
            tmp = new IbisIdentifier[toConnect.size()];
            for (int i = 0; i < toConnect.size(); i++) {
                tmp[i] = toConnect.get(i);
            }
            toConnect.clear();
        }

        // do not keep the lock during connection setup
        for (int i = 0; i < tmp.length; i++) {
            connectSOSendPort(tmp[i]);
        }
    }

    public void handleMyOwnJoin() {
        if(DISABLE_SO_BCAST) return;
    
        if (LABEL_ROUTING_MCAST) {
            omc.addIbis(s.ident);
        }
    }

    public void handleCrash(IbisIdentifier id) {
        if (LABEL_ROUTING_MCAST) {
            omc.removeIbis(id);
            omc.setDestination(s.victims.getIbises());
        }
    }

    protected void exit() {
        if(DISABLE_SO_BCAST) return;
    
        if (LABEL_ROUTING_MCAST) {
            omc.done();
            omcInfo.end();
        }
    }

    static class AsyncBcaster extends Thread {
        private SOCommunication c;

        private SOInvocationRecord r;

        AsyncBcaster(SOCommunication c, SOInvocationRecord r) {
            this.c = c;
            this.r = r;
        }

        public void run() {
            c.doBroadcastSOInvocation(r);
        }
    }
}
