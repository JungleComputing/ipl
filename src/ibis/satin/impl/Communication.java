/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

public abstract class Communication extends SpawnSync {

    //used by GlobalResultTable
    static void connect(SendPort s, ReceivePortIdentifier ident) {
        boolean success = false;
        do {
            try {
                s.connect(ident);
                success = true;
            } catch (IOException e) {
                if (grtLogger.isInfoEnabled()) {
                    grtLogger.info("IOException in connect to " + ident + ": "
                            + e, e);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e2) {
                    // ignore
                }
            }
        } while (!success);
    }

    static boolean connect(SendPort s, ReceivePortIdentifier ident,
            long timeoutMillis) {
        boolean success = false;
        long startTime = System.currentTimeMillis();
        do {
            try {
                s.connect(ident, timeoutMillis);
                success = true;
            } catch (IOException e) {
                if (grtLogger.isInfoEnabled()) {
                    grtLogger.info("IOException in connect to " + ident + ": "
                            + e, e);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e2) {
                    // ignore
                }
            }
        } while (!success
                && System.currentTimeMillis() - startTime < timeoutMillis);
        return success;
    }

    //used by GlobalResultTable
    ReceivePortIdentifier lookup(String portname) throws IOException {
        return ibis.registry().lookupReceivePort(portname);
    }

    SendPort getReplyPortWait(IbisIdentifier id) {
        SendPort s;
        if (ASSERTS) {
            assertLocked(this);
        }

        do {
            s = victims.getReplyPort(id);
            if (s == null) {
                if (commLogger.isDebugEnabled()) {
                    commLogger.debug("SATIN '" + this.ident.name()
                            + "': could not get reply port to " + id.name()
                            + ", retrying");
                }
                try {
                    wait();
                } catch (Exception e) {
                    // Ignore.
                }
            }
        } while (s == null);

        return s;
    }

    SendPort getReplyPortNoWait(IbisIdentifier id) {
        SendPort s;
        if (ASSERTS) {
            assertLocked(this);
        }
        s = victims.getReplyPort(id);
        return s;
    }

    boolean satinPoll() {
        if (POLL_FREQ == 0) { // polling is disabled
            if (HANDLE_MESSAGES_IN_LATENCY) {
                commLogger.fatal("Polling is disabled while messages are "
                        + "handled in the latency.\n"
                        + "This is a configuration error.");
                System.exit(1);
            }
            return false;
        }

        if (upcalls && !upcallPolling) {
            // we are using upcalls, but don't want to poll
            if (HANDLE_MESSAGES_IN_LATENCY) {
                commLogger.fatal("Polling is disabled while messages are "
                        + "handled in the latency.\n"
                        + "This is a configuration error.");
                System.exit(1);
            }
            return false;
        }

        if (POLL_FREQ > 0) {
            long curr = pollTimer.currentTimeNanos();
            if (curr - prevPoll < POLL_FREQ) {
                return false;
            }
            prevPoll = curr;
        }

        if (POLL_TIMING) {
            pollTimer.start();
        }

        ReadMessage m = null;
        if (POLL_RECEIVEPORT) {
            try {
                m = receivePort.poll();
            } catch (IOException e) {
                commLogger.warn("SATIN '" + ident.name()
                        + "': Got Exception while polling: " + e, e);
            }

            if (m != null) {
                messageHandler.upcall(m);
                try {
                    // Finish the message, the upcall does not need to do this.
                    m.finish();
                } catch (Exception e) {
                    commLogger.warn("error in finish: " + e, e);
                }
            }
        } else {
            try {
                ibis.poll(); // does not return message, but triggers upcall.
            } catch (Exception e) {
                commLogger.warn("polling failed, continuing anyway: " + e, e);
            }
        }

        if (POLL_TIMING) {
            pollTimer.stop();
        }

        return m != null;
    }

    void handleDelayedMessages() {
        if (ABORTS) {
            if (gotAborts) {
                handleAborts();
            }
            if (gotExceptions) {
                handleExceptions();
            }
        }
        if (receivedResults) {
            handleResults();
        }
        if (gotActiveTuples) {
            handleActiveTuples();
        }

        if (FAULT_TOLERANCE) {
            if (gotCrashes) {
                handleCrashes();
            }
            if (gotAbortsAndStores) {
                handleAbortsAndStores();
            }
            if (gotDelete) {
                handleDelete();
            }
            if (gotDeleteCluster) {
                handleDeleteCluster();
            }
            if (masterHasCrashed) {
                handleMasterCrash();
            }
            if (clusterCoordinatorHasCrashed) {
                handleClusterCoordinatorCrash();
            }
            if (GRT_MESSAGE_COMBINING) {
                if (updatesToSend) {
                    globalResultTable.sendUpdates();
                }
            }
        }
    }

    /* Only allowed when not stealing. */
    void barrier() {
        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident.name() + "': barrier start");
        }

        // Close the world, no more join and leave upcalls will be received.
        if (!closed) {
            ibis.disableResizeUpcalls();
        }

        int size;
        synchronized (this) {
            size = victims.size();
        }

        try {
            if (master) {
                for (int i = 0; i < size; i++) {
                    ReadMessage r = barrierReceivePort.receive();
                    r.finish();
                }

                for (int i = 0; i < size; i++) {
                    SendPort s;
                    synchronized (this) {
                        s = victims.getPort(i);
                    }
                    WriteMessage writeMessage = s.newMessage();
                    writeMessage.writeByte(Protocol.BARRIER_REPLY);
                    writeMessage.finish();
                }
            } else {
                WriteMessage writeMessage = barrierSendPort.newMessage();
                writeMessage.finish();

                if (!upcalls) {
                    while (!gotBarrierReply/* && !exiting */) {
                        satinPoll();
                    }
                    /*
                     * Imediately reset gotBarrierReply, we know that a reply
                     * has arrived.
                     */
                    gotBarrierReply = false;
                } else {
                    synchronized (this) {
                        while (!gotBarrierReply) {
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                // Ignore.
                            }
                        }

                        /*
                         * Imediately reset gotBarrierReply, we know that a
                         * reply has arrived.
                         */
                        gotBarrierReply = false;
                    }
                }
            }
        } catch (IOException e) {
            commLogger.fatal("SATIN '" + ident.name() + "': error in barrier: "
                    + e, e);
            System.exit(1);
        }

        if (!closed) {
            ibis.enableResizeUpcalls();
        }

        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident.name() + "': barrier DONE");
        }
    }
}
