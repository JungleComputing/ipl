// @@@ msg combining AND lrmc at the same time is not supported

/*
 * Created on Apr 26, 2006 by rob
 */
package ibis.satin.impl.sharedObjects;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.StaticProperties;
import ibis.ipl.WriteMessage;
import ibis.satin.SharedObject;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.communication.Communication;
import ibis.satin.impl.communication.Protocol;
import ibis.satin.impl.loadBalancing.Victim;
import ibis.util.DeepCopy;
import ibis.util.messagecombining.MessageCombiner;
import ibis.util.messagecombining.MessageSplitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import lrmcast.ObjectMulticaster;

final class SOCommunication implements Config, Protocol {
    private static final boolean ASYNC_SO_BCAST = false;

    private final static int LOOKUP_WAIT_TIME = 10000;

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
    private ArrayList toConnect = new ArrayList();

    private HashMap ports = new HashMap();

    private ObjectMulticaster omc;

    protected SOCommunication(Satin s) {
        this.s = s;
    }

    protected void init(StaticProperties requestedProperties) {
        try {
            soPortType = createSOPortType(requestedProperties);
        } catch (Exception e) {
            commLogger.fatal("SATIN '" + s.ident + "': Could not start ibis: "
                + e, e);
            System.exit(1); // Could not start ibis                        
        }

        if (LABEL_ROUTING_MCAST) {
            try {
                omc = new ObjectMulticaster(s.comm.ibis);
            } catch (Exception e) {
                System.err.println("cannot create OMC: " + e);
                e.printStackTrace();
                System.exit(1);
            }

            new SOInvocationReceiver(s, omc).start();
        }
    }

    protected PortType createSOPortType(StaticProperties reqprops)
        throws IOException, IbisException {
        StaticProperties satinPortProperties = new StaticProperties(reqprops);

        if (CLOSED) {
            satinPortProperties.add("worldmodel", "closed");
        } else {
            satinPortProperties.add("worldmodel", "open");
        }

        String commprops = "OneToOne, OneToMany, ExplicitReceipt, Reliable";
        commprops += ", ConnectionUpcalls, ConnectionDowncalls";
        if (UPCALLS) {
            if (UPCALL_POLLING) {
                commprops += ", PollUpcalls";
            } else {
                commprops += ", AutoUpcalls";
            }
        }
        satinPortProperties.add("communication", commprops);

        satinPortProperties.add("serialization", "object");

        return s.comm.ibis.createPortType("satin SO porttype",
            satinPortProperties);
    }

    /**
     * Creates SO receive ports for new Satin instances. Do this first, to make
     * them available as soon as possible.
     */
    protected void createSoPorts(IbisIdentifier[] joiners) {
        for (int i = 0; i < joiners.length; i++) {
            // create a receive port for this guy
            try {
                SOInvocationHandler soInvocationHandler = new SOInvocationHandler(
                    Satin.getSatin());
                ReceivePort rec;
                rec = soPortType.createReceivePort("satin so receive port on "
                    + s.ident.name() + " for " + joiners[i].name(),
                    soInvocationHandler, s.ft.getReceivePortConnectHandler());
                if (SO_MAX_INVOCATION_DELAY > 0) {
                    StaticProperties s = new StaticProperties();
                    s.add("serialization", "ibis");
                    soInvocationHandler.setMessageSplitter(new MessageSplitter(
                        s, rec));
                }
                rec.enableConnections();
                rec.enableUpcalls();

                new SOInvocationHandler(s);

                //Create a multicast port to bcast shared object invocations
                //Connections are established in the join upcall
                soSendPort = soPortType.createSendPort("satin so port on "
                    + s.ident.name(), true);

                if (SO_MAX_INVOCATION_DELAY > 0) {
                    StaticProperties props = new StaticProperties();
                    props.add("serialization", "ibis");
                    soMessageCombiner = new MessageCombiner(props, soSendPort);
                }
            } catch (Exception e) {
                commLogger.fatal("SATIN '" + s.ident
                    + "': Could not start ibis: " + e, e);
                System.exit(1); // Could not start ibis
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
                System.err.println("SATIN '" + s.ident.name()
                    + "': unable to broadcast shared object invocations " + e);
            }

            s.stats.soRealMessageCount++;
            soCurrTotalMessageSize = 0;
            soInvocationsDelayTimer = -1;

            s.stats.broadcastSOInvocationsTimer.stop();
        }
    }

    protected void broadcastSOInvocation(SOInvocationRecord r) {
        if (LABEL_ROUTING_MCAST) {
            doBroadcastSOInvocationLRMC(r);
        } else {
            if (ASYNC_SO_BCAST) {
                // We have to make a copy of the object first, the caller might modify it.
                SOInvocationRecord copy = (SOInvocationRecord) DeepCopy
                    .deepCopy(r);
                new AsyncBcaster(this, copy).start();
            } else {
                doBroadcastSOInvocation(r);
            }
        }
    }

    /** Broadcast an so invocation */
    protected void doBroadcastSOInvocationLRMC(SOInvocationRecord r) {
        long byteCount = 0;
        s.stats.broadcastSOInvocationsTimer.start();
        try {
            IbisIdentifier[] tmp;
            synchronized (s) {
                tmp = s.victims.getIbises();
            }
            byteCount = omc.send(tmp, r);
        } catch (Exception e) {
            System.err.println("WARNING, SOI mcast failed: " + e + " msg: "
                + e.getMessage());
            e.printStackTrace();
        }

        s.stats.soInvocations++;
        s.stats.soRealMessageCount++;
        s.stats.soInvocationsBytes += byteCount;
        s.stats.broadcastSOInvocationsTimer.stop();
    }

    /** Broadcast an so invocation */
    protected void doBroadcastSOInvocation(SOInvocationRecord r) {
        long byteCount = 0;
        WriteMessage w = null;

        s.stats.broadcastSOInvocationsTimer.start();

        connectSendPortToNewReceivers();

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

                if (SO_MAX_INVOCATION_DELAY > 0) {
                    soCurrTotalMessageSize += byteCount;
                } else {
                    s.stats.soRealMessageCount++;
                }
            } catch (IOException e) {
                System.err
                    .println("SATIN '" + s.ident.name()
                        + "': unable to broadcast a shared object invocation: "
                        + e);
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
     * as the SO invocation messages.
     */
    protected void broadcastSharedObject(SharedObject object) {
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
            s.stats.soBroadcastSerializationTimer.start();
            size = w.finish();
            if (SO_MAX_INVOCATION_DELAY > 0) {
                soMessageCombiner.sendAccumulatedMessages();
            }
        } catch (IOException e) {
            System.err.println("SATIN '" + s.ident.name()
                + "': unable to broadcast a shared object: " + e);
        }

        s.stats.soBcasts++;
        s.stats.soBcastBytes += size;
        s.stats.soBroadcastTransferTimer.stop();
    }

    /** Broadcast an so invocation */
    protected void doBroadcastSharedObjectLRMC(SharedObject object) {
        long size = 0;
        s.stats.soBroadcastTransferTimer.start();
        try {
            IbisIdentifier[] tmp;
            synchronized (s) {
                tmp = s.victims.getIbises();
            }

            s.stats.soBroadcastSerializationTimer.start();
            size = omc.send(tmp, object);
            s.stats.soBroadcastSerializationTimer.stop();
        } catch (Exception e) {
            System.err.println("WARNING, SO mcast failed: " + e + " msg: "
                + e.getMessage());
            e.printStackTrace();
        }

        s.stats.soBcasts++;
        s.stats.soBcastBytes += size;
        s.stats.soBroadcastTransferTimer.stop();
    }

    /** Add a new connection to the soSendPort */
    protected void addSOConnection(IbisIdentifier id) {
        synchronized (s) {
            toConnect.add(id);
        }
    }

    /** Remove a connection to the soSendPort */
    protected void removeSOConnection(IbisIdentifier id) {
        Satin.assertLocked(s);
        ReceivePortIdentifier r = (ReceivePortIdentifier) ports.remove(id);

        if (r != null) {
            Communication.disconnect(soSendPort, r);
        }
    }

    /** Fetch a shared object from another node */
    protected void fetchObject(String objectId, IbisIdentifier source)
        throws SOReferenceSourceCrashedException {
        // request the shared object from the source
        try {
            s.lb.setCurrentVictim(source);
            Victim v;
            synchronized (s) {
                v = s.victims.getVictim(source);
            }
            if (v == null) {
                // hm we've got a problem here
                // push the job somewhere else?
                soLogger.error("SATIN '" + s.ident.name() + "': could not "
                    + "write shared-object request");
                throw new SOReferenceSourceCrashedException();
            }

            WriteMessage w = v.newMessage();
            w.writeByte(SO_REQUEST);
            w.writeString(objectId);
            w.finish();
        } catch (IOException e) {
            // hm we've got a problem here
            // push the job somewhere else?
            soLogger.error("SATIN '" + s.ident.name() + "': could not "
                + "write shared-object request", e);
            throw new SOReferenceSourceCrashedException();
        }

        // wait for the reply
        while (true) {
            // handleDelayedMessages();
            synchronized (s) {
                if (s.so.gotObject) {
                    s.so.gotObject = false;
                    s.currentVictimCrashed = false;
                    break;
                }
                if (s.currentVictimCrashed) {
                    s.currentVictimCrashed = false;
                    break;
                }

                try {
                    s.wait();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        if (s.so.sharedObject == null) {
            // the source has crashed, abort the job
            throw new SOReferenceSourceCrashedException();
        }
        synchronized (s) {
            s.so.sharedObjects.put(s.so.sharedObject.objectId,
                s.so.sharedObject);
            s.so.sharedObject = null;
        }
        soLogger.info("SATIN '" + s.ident.name()
            + "': received shared object from " + source);
        // handleDelayedMessages();
    }

    protected void handleSORequests() {
        s.so.gotSORequests = false;
        WriteMessage wm;
        long size;
        IbisIdentifier origin;
        String objid;

        while (true) {
            Victim v;
            synchronized (s) {
                if (s.so.SORequestList.getCount() == 0) return;
                origin = s.so.SORequestList.getRequester(0);
                objid = s.so.SORequestList.getobjID(0);
                s.so.SORequestList.removeIndex(0);
                v = s.victims.getVictim(origin);
            }

            if (v == null) return; // node might have crashed

            s.stats.soTransferTimer.start();

            SharedObject so = s.so.getSOReference(objid);

            if (ASSERTS && so == null) {
                soLogger.fatal("SATIN '" + s.ident.name()
                    + "': EEEK, requested shared object: " + objid
                    + " not found! Exiting..");
                System.exit(1); // Failed assertion
            }

            // No need to hold the lock while writing the object.
            // Updates cannot change the state of the object during the send, 
            // they are delayed until safe a point.
            try {
                wm = v.newMessage();
                wm.writeByte(SO_TRANSFER);

                s.stats.soSerializationTimer.start();
                wm.writeObject(so);
                s.stats.soSerializationTimer.stop();
                size = wm.finish();

                // stats
                s.stats.soTransfers++;
                s.stats.soTransfersBytes += size;

                s.stats.soTransferTimer.stop();
            } catch (IOException e) {
                soLogger.error("SATIN '" + s.ident.name()
                    + "': got exception while sending" + " shared object", e);
            }
        }
    }

    protected void handleSORequest(ReadMessage m) {
        String objid = null;
        IbisIdentifier origin = m.origin().ibis();

        soLogger.info("SATIN '" + s.ident.name() + "': got so request");

        try {
            objid = m.readString();
            // no need to finish the message. We don't do any communication
        } catch (IOException e) {
            soLogger.warn("SATIN '" + s.ident.name()
                + "': got exception while reading" + " shared object request: "
                + e.getMessage());
        }

        synchronized (s) {
            s.so.addToSORequestList(origin, objid);
        }
    }

    protected void handleSOTransfer(ReadMessage m) { // normal so transfer (not exportObject)
        SharedObject obj = null;

        s.stats.soDeserializationTimer.start();
        try {
            obj = (SharedObject) m.readObject();
        } catch (IOException e) {
            soLogger.error("SATIN '" + s.ident.name()
                + "': got exception while reading" + " shared object", e);
        } catch (ClassNotFoundException e) {
            soLogger.error("SATIN '" + s.ident.name()
                + "': got exception while reading" + " shared object", e);
        }
        s.stats.soDeserializationTimer.stop();

        // no need to finish the read message here. 
        // We don't block and don't do any communication
        s.so.receiveObject(obj);
    }

    private void connectSOSendPort(IbisIdentifier ident) {
        ReceivePortIdentifier r = s.comm.lookup_wait(
            "satin so receive port on " + ident.name() + " for "
                + s.ident.name(), LOOKUP_WAIT_TIME);

        if (r == null) {
            soLogger.warn("SATIN '" + s.ident.name()
                + "': unable to lookup SO receive port ");
            // We won't broadcast the object to this receiver.
            // This is not really a problem, it will get the object if it
            // needs it. But the node has probably crashed anyway.
            return;
        }

        // and connect
        if (Communication.connect(soSendPort, r, Satin.CONNECT_TIMEOUT)) {
            synchronized (s) {
                ports.put(ident, r);
            }
        } else {
            soLogger.warn("SATIN '" + s.ident.name()
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
                tmp[i] = (IbisIdentifier) toConnect.get(i);
            }
            toConnect.clear();
        }

        // do not keep the lock during connection setup
        for (int i = 0; i < tmp.length; i++) {
            connectSOSendPort(tmp[i]);
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
