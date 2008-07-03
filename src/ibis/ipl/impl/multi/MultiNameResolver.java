package ibis.ipl.impl.multi;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;

public  class MultiNameResolver implements Runnable {
    private static final Logger logger = Logger.getLogger(MultiNameResolver.class);

    private final MultiIbis ibis;
    private final String ibisName;
    private ReceivePort receivePort;
    private SendPort replyPort;
    private SendPort requestPort;

    private IbisIdentifier toResolve;

    private boolean quit = false;
    private static final String resolvePortName = "ibis.multi.name.resolve";

    private static final byte OPP_REPLY = 1;
    private static final byte OPP_REQUEST = 2;
    private static final byte OPP_QUIT = 0;

    private static HashMap<String, MultiNameResolver> resolverMap = new HashMap<String, MultiNameResolver>();

    public MultiNameResolver(MultiIbis multiIbis, String ibisName) throws IOException {
        this.ibis = multiIbis;
        this.ibisName = ibisName;
        resolverMap.put(ibisName, this);
        Ibis subIbis = ibis.subIbisMap.get(ibisName);
        receivePort = subIbis.createReceivePort(ibis.resolvePortType, resolvePortName);
        receivePort.enableConnections();
        replyPort = subIbis.createSendPort(ibis.resolvePortType);
        requestPort = subIbis.createSendPort(ibis.resolvePortType);
        if (logger.isDebugEnabled()) {
            logger.debug("Started MultiNameResolver for: " + ibisName);
        }
    }

    public void run() {
        do {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Resolver running for: " + ibisName);
                }
                ReadMessage readMessage;
                readMessage = receivePort.receive();
                if (logger.isDebugEnabled()) {
                    logger.debug("Resolver read message for: " + ibisName);
                }
                byte operation = readMessage.readByte();
                switch (operation) {
                case OPP_REQUEST:
                    IbisIdentifier requestor = readMessage.origin().ibisIdentifier();
                    readMessage.finish();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Sending Reply For: " + ibisName + " from: " + requestor);
                    }
                    replyPort.connect(requestor, resolvePortName);
                    WriteMessage sendMessage = replyPort.newMessage();
                    sendMessage.writeByte(OPP_REPLY);
                    sendMessage.writeObject(ibis.id);
                    sendMessage.finish();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Disconnecting Reply For: " + ibisName + " from: " + requestor);
                    }
                    replyPort.disconnect(requestor, resolvePortName);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Reply Complete for: " + ibisName + " from: " + requestor);
                    }
                    break;
                case OPP_REPLY:
                    if (logger.isDebugEnabled()) {
                        logger.debug("Received Reply For: " + ibisName);
                    }
                    MultiIbisIdentifier id = (MultiIbisIdentifier) readMessage.readObject();
                    readMessage.finish();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Setting Resolved for: " + ibisName + " id: " + id);
                    }
                    ibis.resolved(id);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Locking for: " + ibisName);
                    }
                    synchronized (this) {
                        toResolve = null;
                        if (logger.isDebugEnabled()) {
                            logger.debug("Notifying for resolution: " + ibisName + " on: " + this);
                        }
                        notifyAll();
                    }
                    break;
                case OPP_QUIT:
                    readMessage.finish();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Resolver quitting for: " + ibisName);
                    }
                    quit = true;
                    break;
                }            }
            catch (IOException e) {
                // TODO What do we do here now?
                logger.error("Got IOException while resolving: " + e);
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                // TODO What do we do here now?
                logger.error("Got ClassNotFoundException while resolving: " + e);
                e.printStackTrace();
            }
        } while(!quit);
    }

    public static void quit() {
        // TODO Wake up all threads.
    }

    public static void resolve(IbisIdentifier toResolve, String ibisName) throws IOException {
        MultiNameResolver resolver = resolverMap.get(ibisName);
        if (logger.isDebugEnabled()) {
            logger.debug("Making Resolve Request for: " + ibisName);
        }
        synchronized(resolver) {
            resolver.toResolve = toResolve;
            resolver.requestPort.connect(toResolve, resolvePortName);
            if (logger.isDebugEnabled()) {
                logger.debug("Sending Request for: " + ibisName);
            }
            WriteMessage writeMessage = resolver.requestPort.newMessage();
            writeMessage.writeByte(OPP_REQUEST);
            if (logger.isDebugEnabled()) {
                logger.debug("Finishing Request for: " + ibisName);
            }
            writeMessage.finish();
            while (resolver.toResolve != null) {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Waiting For Resolution For: " + ibisName + " on: " + resolver);
                    }
                    resolver.wait();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Resolution Complete for: " + ibisName);
                    }
                }
                catch (InterruptedException e) {
                    // Ignored
                }
            }
            // Disconnect has to come after wait or we end up in deadlock.
            if (logger.isDebugEnabled()) {
                logger.debug("Disconnecting Request for: " + ibisName);
            }
            resolver.requestPort.disconnect(toResolve, resolvePortName);
        }
    }
}