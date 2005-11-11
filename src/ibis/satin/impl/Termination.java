/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.WriteMessage;

import java.io.IOException;

public abstract class Termination extends Initialization {

    private void bcastMessage(byte opcode) {
        int size = 0;
        synchronized (this) {
            size = victims.size();
            //System.err.println("victims size: " + size);
        }

        for (int i = 0; i < size; i++) {
            Victim v = null;
            try {
                WriteMessage writeMessage;
                synchronized (this) {
                    if (commLogger.isDebugEnabled()) {
                        commLogger.debug("SATIN '" + ident
                            + "': sending exit message to "
                            + victims.getVictim(i).ident);
                    }

                    //System.err.println("victims size: " + victims.size()
                    // + ",i: " + i);

                    v = victims.getVictim(i);
                }

                writeMessage = v.newMessage();
                writeMessage.writeByte(opcode);
                writeMessage.finish();
            } catch (IOException e) {
                synchronized (this) {
                    System.err.println("SATIN: Could not send bcast "
                        + "message to " + victims.getVictim(i).ident);
                }
            }
        }
    }

    /**
     * Called at the end of the rewritten "main", to do a synchronized exit.
     */
    public void exit() {
        /* send exit messages to all others */
        int size;

        totalTimer.stop();

        if (!closed) {
            ibis.disableResizeUpcalls();
        }

        if (stats && detailedStats) {
            printDetailedStats();
        }

        connectionUpcallsDisabled = true;

        synchronized (this) {
            size = victims.size();
            //System.err.println("victims size: " + size);
        }

        if (master) {
            exiting = true;
            // algorithm.exit(); // give the algorithm time to clean up

            bcastMessage(Protocol.EXIT);
            
            // wait until everybody has send an ACK
            if (upcalls) {
                synchronized (this) {
                    while (exitReplies != size) {
                        try {
                            wait();
                        } catch (Exception e) {
                            // Ignore.
                        }
                    }

                }
            } else {
                while (exitReplies != size) {
                    satinPoll();
                }
            }

            // OK, we have got the ack from everybody, 
            // now we know that there will be no further communication between nodes.
            // Broadcast this again.
            
            bcastMessage(Protocol.EXIT_STAGE2);
        } else { // send exit ack to master
            Victim mp = null;

            synchronized (this) {
                mp = getVictimWait(masterIdent);
            }

            try {
                WriteMessage writeMessage;
                if (commLogger.isDebugEnabled()) {
                    commLogger.debug("SATIN '" + ident
                        + "': sending exit ACK message to " + masterIdent);
                }

                writeMessage = mp.newMessage();
                writeMessage.writeByte(Protocol.EXIT_REPLY);
                if (stats) {
                    writeMessage.writeObject(createStats());
                }
                writeMessage.finish();
            } catch (IOException e) {
                System.err.println("SATIN: Could not send exit message to "
                    + masterIdent);
            }

            if (upcalls) {
                synchronized (this) {
                    while (!exitStageTwo) {
                        try {
                            wait();
                        } catch (Exception e) {
                            // Ignore.
                        }
                    }
                }
            } else {
                while (!exitStageTwo) {
                    satinPoll();
                }
            }
        }

        // OK, we have got the ack from everybody, 
        // now we know that there will be no further communication between nodes.
        
//        barrier(); // Wait until everybody agrees to exit.

        algorithm.exit(); // give the algorithm time to clean up

        if (master && stats) {
            printStats();
        }

        // System.exit(1);

        try {
            if (SUPPORT_TUPLE_MULTICAST) {
                tuplePort.close();
            }
        } catch (Throwable e) {
            System.err.println("tuplePort.close() throws " + e);
            e.printStackTrace();
        }

        // If not closed, free ports. Otherwise, ports will be freed in leave
        // calls.
        while (true) {
            try {
                Victim v;

                synchronized (this) {
                    if (victims.size() == 0) {
                        break;
                    }

                    v = victims.getVictim(0);

                    if (commLogger.isDebugEnabled()) {
                        commLogger.debug("SATIN '" + ident
                            + "': closing sendport to "
                            + victims.getVictim(0).ident);
                    }
                }

                if (v != null) {
                    v.close();
                }

                victims.remove(0);

            } catch (Throwable e) {
                System.err.println("port.close() throws " + e);
            }
        }

        try {
            receivePort.close();
            if (SUPPORT_TUPLE_MULTICAST) {
                tupleReceivePort.close();
            }
        } catch (Throwable e) {
            System.err.println("port.close() throws " + e);
        }

        if (FAULT_TOLERANCE && !FT_NAIVE) {
            globalResultTable.exit();
        }

        try {
            ibis.end();
        } catch (Throwable e) {
            System.err.println("ibis.end throws " + e);
        }

        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident + "': exited");
        }

        // Do a gc, and run the finalizers. Useful for printing statistics in
        // Satin applications.
        // The app should register a shutdownhook. --Rob
        System.gc();
        System.runFinalization();
        // System.runFinalizersOnExit(true); // depricated

        System.exit(0); // Needed for IBM jit.
    }
}
