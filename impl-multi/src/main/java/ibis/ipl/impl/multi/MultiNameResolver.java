/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibis.ipl.impl.multi;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public  class MultiNameResolver {
    private static final Logger logger = LoggerFactory.getLogger(MultiNameResolver.class);

    private final MultiIbis ibis;
    private final String ibisName;
    private ReceivePort requestListenPort;
    private ReceivePort replyListenPort;
    private SendPort replyPort;
    private SendPort requestPort;

    private HashMap<Integer, IbisIdentifier> resolveQueue = new HashMap<Integer, IbisIdentifier>();

    private boolean quit = false;
    private static final String resolvePortName = "ibis.multi.name.resolve";
    private static final String replyPortName = "ibis.multi.name.resolveReply";

    private static final byte OPP_REPLY = 1;
    private static final byte OPP_REQUEST = 2;
    private static final byte OPP_QUIT = 0;

    public MultiNameResolver(MultiIbis multiIbis, String ibisName) throws IOException {
        this.ibis = multiIbis;
        this.ibisName = ibisName;
        ibis.resolverMap.put(ibisName, this);
        Ibis subIbis = ibis.subIbisMap.get(ibisName);
        requestListenPort = subIbis.createReceivePort(MultiIbis.resolvePortType, resolvePortName);
        requestListenPort.enableConnections();
        replyListenPort = subIbis.createReceivePort(MultiIbis.resolvePortType, replyPortName);
        replyListenPort.enableConnections();
        replyPort = subIbis.createSendPort(MultiIbis.resolvePortType);
        requestPort = subIbis.createSendPort(MultiIbis.resolvePortType);
        if (logger.isDebugEnabled()) {
            logger.debug("Started MultiNameResolver for: " + ibisName);
        }
        ThreadPool.createNew(new RequestHandler(), "Request Listener: " + ibisName);
        ThreadPool.createNew(new ReplyHandler(), "Reply Listener: " + ibisName);
    }

    private void waitForId() {
        // Wait until we have an id set to share
        if (logger.isDebugEnabled()) {
            logger.debug("Waiting for id:" + ibisName);
        }
        while (ibis.id == null) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Ignored
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Got id:" + ibisName);
        }
    }

    private class ReplyHandler implements Runnable {
        public void run() {
            waitForId();
            do {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("ReplyHandler running for: " + ibisName);
                    }
                    ReadMessage readMessage;
                    readMessage = replyListenPort.receive();
                    if (logger.isDebugEnabled()) {
                        logger.debug("ReplyHandler read message for: " + ibisName);
                    }
                    byte operation = readMessage.readByte();
                    switch (operation) {
                    case OPP_REPLY:
                        if (logger.isDebugEnabled()) {
                            logger.debug("Received Reply For: " + ibisName);
                        }
                        int hashCode = readMessage.readInt();
                        MultiIbisIdentifier id = (MultiIbisIdentifier) readMessage.readObject();
                        readMessage.finish();
                        if (logger.isDebugEnabled()) {
                            logger.debug("Setting Resolved for: " + ibisName + " id: " + id);
                        }
                        ibis.resolved(id);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Locking for: " + ibisName);
                        }
                        synchronized (resolveQueue) {
                            IbisIdentifier toResolve = resolveQueue.remove(new Integer(hashCode));
                            synchronized(toResolve) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Notifying for resolution: " + ibisName + " on: " + this);
                                }
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Notifying for resolution: " + ibisName + " on: " + this);
                                }
                                toResolve.notifyAll();
                            }
                        }
                        break;
                    case OPP_QUIT:
                        if (logger.isDebugEnabled()) {
                            logger.debug("Resolver quitting for: " + ibisName);
                        }
                        readMessage.finish();
                        quit = true;
                        break;
                    default:
                        if (logger.isDebugEnabled()) {
                            logger.debug("Unknown request for: " + ibisName);
                        }
                    readMessage.finish();
                    break;
                    }
                }
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
    }

    private class RequestHandler implements Runnable {
        public void run() {
            waitForId();
            do {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Resolver running for: " + ibisName);
                    }
                    ReadMessage readMessage;
                    readMessage = requestListenPort.receive();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Resolver read message for: " + ibisName);
                    }
                    byte operation = readMessage.readByte();
                    switch (operation) {
                    case OPP_REQUEST:
                        if (logger.isDebugEnabled()) {
                            logger.debug("Processing Request for: " + ibisName);
                        }
                        int hashCode = readMessage.readInt();
                        IbisIdentifier requestor = readMessage.origin().ibisIdentifier();
                        readMessage.finish();
                        if (logger.isDebugEnabled()) {
                            logger.debug("Sending Reply For: " + ibisName + " from: " + requestor + " id:" + ibis.id);
                        }
                        replyPort.connect(requestor, replyPortName);
                        WriteMessage sendMessage = replyPort.newMessage();
                        sendMessage.writeByte(OPP_REPLY);
                        sendMessage.writeInt(hashCode);
                        sendMessage.writeObject(ibis.id);
                        sendMessage.finish();
                        if (logger.isDebugEnabled()) {
                            logger.debug("Disconnecting Reply For: " + ibisName + " from: " + requestor);
                        }
                        replyPort.disconnect(requestor, replyPortName);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Reply Complete for: " + ibisName + " from: " + requestor);
                        }
                        break;
                    case OPP_QUIT:
                        readMessage.finish();
                        if (logger.isDebugEnabled()) {
                            logger.debug("Resolver quitting for: " + ibisName);
                        }
                        quit = true;
                        break;
                    default:
                        if (logger.isDebugEnabled()) {
                            logger.debug("Unknown request for: " + ibisName);
                        }
                        readMessage.finish();
                        break;
                    }
                }
                catch (IOException e) {
                    // TODO What do we do here now?
                    logger.error("Got IOException while resolving: " + e);
                    e.printStackTrace();
                }
            } while(!quit);
        }
    }

    public static void quit() {
        // TODO Wake up all threads.
    }

    public void resolve(IbisIdentifier toResolve, String ibisName) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Making Resolve Request for: " + ibisName);
        }
        Integer id = new Integer(toResolve.hashCode());
        synchronized (resolveQueue) {
            // Make sure we don't collide
            while (resolveQueue.get(id) != null) {
                try {
                    resolveQueue.wait();
                } catch (InterruptedException e) {
                    // Ignored
                }
            }
            resolveQueue.put(id, toResolve);
        }
        synchronized (toResolve) {
            this.requestPort.connect(toResolve, resolvePortName);
            if (logger.isDebugEnabled()) {
                logger.debug("Sending Request for: " + ibisName);
            }
            WriteMessage writeMessage = this.requestPort.newMessage();
            writeMessage.writeByte(OPP_REQUEST);
            writeMessage.writeInt(toResolve.hashCode());
            if (logger.isDebugEnabled()) {
                logger.debug("Finishing Request for: " + ibisName);
            }
            writeMessage.finish();
            if (logger.isDebugEnabled()) {
                logger.debug("Disconnecting Request for: " + ibisName);
            }
            this.requestPort.disconnect(toResolve, resolvePortName);

            while (!ibis.isResolved(toResolve)) {
                try {
                    // Wait for the resolution to finish.
                    if (logger.isDebugEnabled()) {
                        logger.debug("Waiting For Resolution For: " + ibisName + " on: " + this);
                    }
                    toResolve.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Resolution Complete for: " + ibisName);
            }
        }
    }
}
