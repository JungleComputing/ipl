package ibis.ipl.impl.stacking.lrmc;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.impl.stacking.lrmc.io.MessageReceiver;
import ibis.ipl.impl.stacking.lrmc.util.DynamicObjectArray;
import ibis.ipl.impl.stacking.lrmc.util.IbisSorter;
import ibis.ipl.impl.stacking.lrmc.util.Message;
import ibis.ipl.impl.stacking.lrmc.util.MessageCache;
import ibis.ipl.impl.stacking.lrmc.util.MessageQueue;
import ibis.util.TypedProperties;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LabelRoutingMulticast extends Thread implements MessageUpcall {

    private final static int ZOMBIE_THRESHOLD = 10000;

    private static final Logger logger = LoggerFactory
            .getLogger(LabelRoutingMulticast.class);

    final LrmcIbis ibis;

    private final String name;

    ReceivePort receive;

    private MessageReceiver receiver;

    private final MessageCache cache;

    private final DynamicObjectArray<SendPort> sendports = new DynamicObjectArray<SendPort>();
    private final DynamicObjectArray<Long> diedmachines = new DynamicObjectArray<Long>();

    private boolean finish = false;

    private int[] destinations = null;

    private long bytes = 0;

    private MessageQueue sendQueue;

    public LabelRoutingMulticast(LrmcIbis ibis, MessageReceiver m,
            MessageCache c, String name) throws IOException {
        this.ibis = ibis;
        this.receiver = m;
        this.name = name;
        this.cache = c;
        this.sendQueue = new MessageQueue(
                new TypedProperties(ibis.properties()).getIntProperty(
                        "lrmc.queueSize", 32));
        receive = ibis.base.createReceivePort(LrmcIbis.additionalPortType, "LRMCRing-"
                + name, this);
        receive.enableConnections();
        receive.enableMessageUpcalls();

        super.setName("LabelRoutingMulticast:" + name);
        this.start();
    }

    public static PortType getPortType() {
        return new PortType(PortType.SERIALIZATION_DATA,
                PortType.COMMUNICATION_RELIABLE,
                PortType.CONNECTION_MANY_TO_ONE, PortType.RECEIVE_AUTO_UPCALLS);
    }

    private synchronized SendPort getSendPort(int id) {

        if (id == -1) {
            logger.info("Ignoring " + id);
            return null;
        }

        SendPort sp = sendports.get(id);

        if (sp == null) {
            // We're not connect to this ibis yet, so connect and store for
            // later use.

            // Test if the machine died recently to prevent us from trying to
            // connect over and over again (this may be a problem since a single
            // large mcast may be fragmented into many small packets, each with
            // the same route containing the dead machine)
            Long ripTime = diedmachines.get(id);

            if (ripTime != null) {

                long now = System.currentTimeMillis();

                if (now - ripTime.longValue() > ZOMBIE_THRESHOLD) {
                    // the machine has been dead for a long time, but the sender
                    // insists it is still alive. Lets try again and see what
                    // happens.
                    diedmachines.remove(id);

                    logger.info("Sender insists that " + id
                            + " is still allive, so I'll try again!");
                } else {
                    logger.info("Ignoring " + id + " since it's dead!");
                    return null;
                }
            }

            boolean failed = false;
            IbisIdentifier ibisID = null;

            try {
                sp = ibis.base.createSendPort(LrmcIbis.additionalPortType);
                ibisID = ibis.getId(id);

                if (ibisID != null) {
                    sp.connect(ibisID, "LRMCRing-" + name, 10000, true);
                    sendports.put(id, sp);
                } else {
                    logger.info("No Ibis yet at position " + id);
                    failed = true;
                }
            } catch (IOException e) {
                failed = true;
                logger.info("Got exception ", e);
            }

            if (failed) {
                logger.info("Failed to connect to " + id
                        + " - informing nameserver!");

                // notify the nameserver that this machine may be dead...
                try {
                    if (ibisID != null) {
                        ibis.registry().maybeDead(ibisID);
                    }
                    diedmachines.put(id, new Long(System.currentTimeMillis()));
                } catch (Exception e2) {
                    logger.info("Failed to inform nameserver! " + e2);
                    // ignore
                }

                logger.info("Done informing nameserver");
                return null;
            }
        }

        return sp;
    }

    private void internalSend(Message m) {
        SendPort sp = null;
        if (m.destinationsUsed == 0) {
            if (m.last) {
                sp = getSendPort(m.sender);
                if (sp != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Writing DONE message " + m.id
                                + " to sender " + m.sender);
                    }
                    try {
                        WriteMessage wm = sp.newMessage();
                        wm.writeInt(-1);
                        wm.writeInt(m.id);
                        wm.finish();
                    } catch (IOException e) {
                        logger.debug("Writing DONE message to " + m.sender
                                + " failed");
                    }
                } else if (logger.isDebugEnabled()) {
                    logger.debug("No sendport for sender " + m.sender);
                }
            }
            return;
        }

        // Get the next target from the destination array. If this fails, get
        // the next one, etc. If no working destination is found we give up.
        int index = 0;
        int id = -1;

        do {
            id = m.destinations[index++];
            sp = getSendPort(id);
            if (sp == null) {
                synchronized (this) {
                    if (finish) {
                        return;
                    }
                }
            }
        } while (sp == null && index < m.destinationsUsed);

        try {
            if (sp == null) {
                // No working destinations where found, so give up!
                logger.info("No working destinations found, giving up!");
                return;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Writing message " + m.id + "/" + m.num + " to "
                        + id + ", sender " + m.sender
                        + ", destinations left = "
                        + (m.destinationsUsed - index));
            }

            // send the message to the target
            WriteMessage wm = sp.newMessage();
            m.write(wm, index);
            bytes += wm.finish();
        } catch (IOException e) {
            logger.info("Write to " + id + " failed! ", e);
            sendports.remove(id);
        }
    }

    public void setDestination(IbisIdentifier[] destinations) {

        logger.debug("setDestination called, destinations.length = "
                + destinations.length, new Throwable());

        // We are allowed to change the order of machines in the destination
        // array. This can be used to make the mcast 'cluster aware'.
        IbisSorter.sort(ibis.identifier(), destinations);

        this.destinations = new int[destinations.length];

        for (int i = 0; i < destinations.length; i++) {
            this.destinations[i] = ibis.getIbisID(destinations[i]);
            logger.debug("  " + i + " (" + destinations[i] + " at "
                    + destinations[i].location().getParent() + ") -> "
                    + this.destinations[i]);
        }
    }

    public long getBytes(boolean reset) {

        long tmp = bytes;

        if (reset) {
            bytes = 0;
        }

        return tmp;
    }

    public boolean send(Message m) {

        int[] destOld = m.destinations;

        m.destinations = destinations;
        m.destinationsUsed = destinations.length;
        m.sender = ibis.myID;
        m.local = true;

        internalSend(m);

        m.destinations = destOld;
        return true;
    }

    public void run() {

        while (true) {
            Message m = sendQueue.dequeue();
            if (m == null) {
                // Someone wants us to stop
                return;
            }

            try {
                internalSend(m);
            } catch (Exception e) {
                logger.info("Sender thread got exception! ", e);
            } finally {
                cache.put(m);
            }
        }
    }

    public void done() {
        // sendQueue.printTime();
        synchronized (this) {
            finish = true;
        }
        sendQueue.terminate();
        try {
            join(10000);
        } catch (Exception e) {
            // ignored
        }
        try {
            receive.disableConnections();

            int last = sendports.last();

            for (int i = 0; i < last; i++) {
                SendPort tmp = sendports.get(i);

                if (tmp != null) {
                    tmp.close();
                }
            }

            receive.close(1000);
        } catch (Throwable e) {
            // ignore, we tried...
        }
    }

    public void upcall(ReadMessage rm) throws IOException {

        Message message = null;

        try {
            int len = rm.readInt();
            if (len == -1) {
                // DONE message
                int id = rm.readInt();
                if (logger.isDebugEnabled()) {
                    logger.debug("Got DONE for message " + id);
                }
                receiver.gotDone(id);
                return;
            }

            int dst = rm.readInt();

            message = cache.get(len);
            message.read(rm, len, dst);

            if (logger.isDebugEnabled()) {
                logger.debug("Reading message " + message.id + "/"
                        + message.num + " from " + message.sender);
            }

            if (!message.local) {
                message.refcount++;
                try {
                    receiver.gotMessage(message);
                } catch (Throwable e) {
                    logger.info("Delivery failed! ", e);
                }
            }

            // Is this OK? sendQueue may block (is not allowed in upcall)!
            // However, calling finish() here may change the message order,
            // so we cannot do that. (Ceriel).
            sendQueue.enqueue(message);

        } catch (IOException e) {
            logger.info("Failed to receive message: ", e);
            rm.finish(e);

            if (message != null) {
                cache.put(message);
            }
        }
    }

    public int getPrefferedMessageSize() {
        return cache.getPrefferedMessageSize();
    }
}
