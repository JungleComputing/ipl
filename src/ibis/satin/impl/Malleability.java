/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;

import java.io.IOException;
import java.util.ArrayList;

public abstract class Malleability extends FaultTolerance {

    static ArrayList joiners = new ArrayList();

    private static boolean joinThreadRunning = false;

    void handleJoins(IbisIdentifier[] joiners) {
        String[] names = new String[joiners.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = "satin port on " + joiners[i].name();
        }
        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident + "': dealing with "
                    + names.length + " joins");
        }
        if (SHARED_OBJECTS) {
            createSoReceivePorts(joiners);
        }
        ReceivePortIdentifier[] r = null;
        try {
            r = lookup(names);
            for (int i = 0; i < r.length; i++) {
                IbisIdentifier joiner = joiners[i];
                SendPort s = portType.createSendPort("satin sendport");

                if (FAULT_TOLERANCE) {
                    if (! SCALABLE && !connect(s, r[i], connectTimeout)) {
                        if (commLogger.isDebugEnabled()) {
                            commLogger.debug("SATIN '" + ident
                                    + "': unable to connect to " + joiner
                                    + ", might have crashed");
                        }
                        continue;
                    }
                } else if (!SCALABLE) {
                    connect(s, r[i]);
                }

                synchronized (this) {
                    if (SHARED_OBJECTS) {
                        addSOConnection(joiner);
                    }
                    if (FAULT_TOLERANCE && !FT_NAIVE) {
                        globalResultTable.addReplica(joiner);
                    }
                    victims.add(new Victim(joiner, s, r[i]));
                    notifyAll();
                }

                if (commLogger.isDebugEnabled()) {
                    commLogger.debug("SATIN '" + ident + "': "
                            + joiner + " JOINED");
                }
            }
        } catch (Exception e) {
            commLogger.error("SATIN '" + ident
                    + "': got an exception in Satin.join", e);
            // System.exit(1);
        }
    }

    public void joined(IbisIdentifier joiner) {
        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident + "': got join of " + joiner);
        }

        if (! joinThreadRunning) {
            joinThreadRunning = true;
            Thread p = new Thread("Join thread") {
                public void run() {
                    while(true) {
                        IbisIdentifier[] j = null;
                        synchronized(joiners) {
                            if (joiners.size() != 0) {
                                j = (IbisIdentifier[]) joiners.toArray(new IbisIdentifier[0]);
                                joiners.clear();
                            }
                        }
                        if (j != null && j.length != 0) {
                            handleJoins(j);
                            synchronized(joiners) {
                                joiners.notifyAll();
                            }
                        }
                        try {
                            Thread.sleep(1000);
                        } catch(Exception e) {
                            // ignored
                        }
                    }
                }
            };

            p.setDaemon(true);
            p.start();
        }

        if (joiner.name().equals("ControlCentreIbis")) {
            return;
        }

        allIbises.add(joiner);

        if (joiner.equals(ident)) {
            synchronized(joiners) {
                while (joiners.size() != 0) {
                    try {
                        joiners.wait();
                    } catch(Exception e) {
                        // ignored
                    }
                }
            }
            return;
        }

        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident + "': '" + joiner
                    + "' from cluster '" + joiner.cluster()
                    + "' is trying to join");
        }

        synchronized(joiners) {
            joiners.add(joiner);
        }

        /*
         * synchronized (this) {
         *     System.err.println("SATIN '" + ident + "': '"
         *             + victims.size() + " hosts joined");
         * }
         */
    }

    public void died(IbisIdentifier corpse) {
        if (ftLogger.isDebugEnabled()) {
            ftLogger.debug("SATIN '" + ident + "': " + corpse
                    + " died");
        }

        left(corpse);
        gotCrashes = true;
    }

    public void left(IbisIdentifier leaver) {
        if (leaver.equals(ident)) {
            return;
        }

        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident + "': " + leaver
                    + " left");
        }

        Victim v;

        synchronized (this) {
            /*
             * if (FAULT_TOLERANCE && !FT_NAIVE) {
             *     globalResultTable.removeReplica(leaver);
             * }
             */
            if (FAULT_TOLERANCE) {
                /* 
                 * master and cluster coordinators will be reelected
                 * only if their crash was confirmed by the nameserver
                 */
                if (leaver.equals(masterIdent)) {
                    masterHasCrashed = true;
                    gotCrashes = true;
                }
                if (leaver.equals(clusterCoordinatorIdent)) {
                    clusterCoordinatorHasCrashed = true;
                    gotCrashes = true;
                }
            } 

            if (SHARED_OBJECTS) {
                removeSOConnection(leaver);
            }
             
            v = victims.remove(leaver);
            notifyAll();

            if (v != null) {
                try {
                    v.close();
                } catch (IOException e) {
                    commLogger.error("SATIN '" + ident
                            + "': port.close() throws exception", e);
                }
            }
        }
    }
}
