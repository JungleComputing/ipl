/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

public abstract class Communication extends SpawnSync {

    static void connect(SendPort s, ReceivePortIdentifier ident) {
        boolean success = false;
        do {
            try {
                s.connect(ident);
                success = true;
            } catch (AlreadyConnectedException x) {
                return;
            } catch (IOException e) {
                commLogger.info(
                    "IOException in connect to " + ident + ": " + e, e);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e2) {
                    // ignore
                }
            }
        } while (!success);
    }

    static void disconnect(SendPort s, ReceivePortIdentifier ident) {
        try {
            s.disconnect(ident);
        } catch (IOException e) {
            // ignored
        }
    }

    static boolean connect(SendPort s, ReceivePortIdentifier ident,
            long timeoutMillis) {
        boolean success = false;
        long startTime = System.currentTimeMillis();
        do {
            try {
                s.connect(ident, timeoutMillis);
                success = true;
            } catch (AlreadyConnectedException x) {
                    return true;
            } catch (IOException e) {
                commLogger.info(
                    "IOException in connect to " + ident + ": " + e, e);
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

    ReceivePortIdentifier lookup(String portname) throws IOException {
        return ibis.registry().lookupReceivePort(portname);
    }

    ReceivePortIdentifier[] lookup(String[] portnames) throws IOException {
        return ibis.registry().lookupReceivePorts(portnames);
    }

    ReceivePortIdentifier lookup_wait(String portname, long timeoutMillis) {
        ReceivePortIdentifier rpi = null;
        try {
            rpi = ibis.registry().lookupReceivePort(portname, timeoutMillis);
        } catch (Exception e) {
            // ignored
        }
        return rpi;
    }

    Victim getVictimWait(IbisIdentifier id) {
        if (ASSERTS) {
            assertLocked(this);
        }

        Victim v = null;

        do {
            v = victims.getVictim(id);
            if (v == null) {
                if (commLogger.isDebugEnabled()) {
                    commLogger.debug("SATIN '" + ident
                            + "': could not get reply port to " + id
                            + ", retrying");
                }
                try {
                    wait();
                } catch (Exception e) {
                    // Ignore.
                }
            }
        } while (v == null);

        return v;
    }

    Victim getVictimNoWait(IbisIdentifier id) {

        if (ASSERTS) {
            assertLocked(this);
        }

        return victims.getVictim(id);
    }

    final protected boolean satinPoll() {
        if (POLL_FREQ == 0) { // polling is disabled
            if (HANDLE_MESSAGES_IN_LATENCY) {
                commLogger.fatal("Polling is disabled while messages are "
                    + "handled in the latency.\n"
                    + "This is a configuration error.");
                System.exit(1);         // Configuration error
            }
            return false;
        }

        if (upcalls && !upcallPolling) {
            // we are using upcalls, but don't want to poll
            if (HANDLE_MESSAGES_IN_LATENCY) {
                commLogger.fatal("Polling is disabled while messages are "
                    + "handled in the latency.\n"
                    + "This is a configuration error.");
                System.exit(1);         // Configuration error
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
                commLogger.warn("SATIN '" + ident
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

    protected final void handleDelayedMessages() {
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

        if (SHARED_OBJECTS) {
            if (gotSOInvocations) {
                handleSOInvocations();
            }
            if (soInvocationsDelay > 0) {
                sendAccumulatedSOInvocations();
            }
        }
    }

    /* Only allowed when not stealing. */
    void barrier() {
        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident + "': barrier start");
        }

        // Close the world, no more join and leave upcalls will be received.
        /*        if (!closed) {
         ibis.disableResizeUpcalls();
         }*/

        int size;
        synchronized (this) {
            size = victims.size();
        }

        try {
            if (master) {
                synchronized (this) {
                    while (barrierRequests != size) {
                        try {
                            wait();
                        } catch (Exception e) {
                            // ignore
                        }
                    }

                    barrierRequests = 0;
                }

                for (int i = 0; i < size; i++) {
                    Victim v;
                    synchronized (this) {
                        v = victims.getVictim(i);
                    }

                    WriteMessage writeMessage = v.newMessage();
                    writeMessage.writeByte(Protocol.BARRIER_REPLY);
                    writeMessage.finish();
                }
            } else {
                Victim v = victims.getVictim(masterIdent);

                WriteMessage writeMessage = v.newMessage();
                writeMessage.writeByte(Protocol.BARRIER_REQUEST);
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
            commLogger.warn("SATIN '" + ident + "': error in barrier", e);
        }

        /*        if (!closed) {
         ibis.enableResizeUpcalls();
         }*/

        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident + "': barrier DONE");
        }
    }
}
