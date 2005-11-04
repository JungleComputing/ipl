/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;

import java.io.IOException;
import java.util.ArrayList;

public abstract class Malleability extends FaultTolerance {

    private static ArrayList joiners = new ArrayList();

    private static boolean joinThreadRunning = false;

    private void handleJoins(IbisIdentifier[] joiners) {
        String[] names = new String[joiners.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = "satin port on " + joiners[i].name();
        }
        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident + "': dealing with "
                    + names.length + " joins");
        }
        ReceivePortIdentifier[] r = null;
        try {
            r = lookup(names);
            for (int i = 0; i < r.length; i++) {
                IbisIdentifier joiner = joiners[i];
                SendPort s = portType.createSendPort("satin sendport");

                if (FAULT_TOLERANCE) {
                    if (!connect(s, r[i], connectTimeout)) {
                        if (commLogger.isDebugEnabled()) {
                            commLogger.debug("SATIN '" + ident
                                    + "': unable to connect to " + joiner
                                    + ", might have crashed");
                        }
                        return;
                    }
                } else {
                    connect(s, r[i]);
                }

                synchronized (this) {
                    if (SHARED_OBJECTS) {
                        addSOConnection(joiner);
                    }
                    if (FAULT_TOLERANCE && !FT_NAIVE) {
                        globalResultTable.addReplica(joiner);
                    }
                    victims.add(joiner, s);
                    notifyAll();
                }

                if (commLogger.isDebugEnabled()) {
                    commLogger.debug("SATIN '" + ident + "': "
                            + joiner + " JOINED");
                }
            }
        } catch (Exception e) {
            System.err.println("SATIN '" + ident
                    + "': got an exception in Satin.join: " + e);
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public void joined(IbisIdentifier joiner) {

        if (! joinThreadRunning) {
            joinThreadRunning = true;
            Thread p = new Thread("Join thread") {
                public void run() {
                    for (;;) {
                        IbisIdentifier[] j = new IbisIdentifier[0];
                        synchronized(joiners) {
                            if (joiners.size() != 0) {
                                j = (IbisIdentifier[]) joiners.toArray(j);
                                joiners.clear();
                            }
                        }
                        if (j.length != 0) {
                            handleJoins(j);
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

        // System.err.println("SATIN '" + ident + "': '" + joiner
        //         + " is joining");

        if (joiner.name().equals("ControlCentreIbis")) {
            return;
        }

        allIbises.add(joiner);

        if (joiner.equals(ident)) {
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
        left(corpse);
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
             
            v = victims.remove(leaver);
            notifyAll();

            if (v != null && v.s != null) {
                try {
                    v.s.close();
                } catch (IOException e) {
                    System.err.println("port.close() throws " + e);
                }
            }
        }
    }
}
