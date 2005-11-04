/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisError;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;

import java.util.HashMap;
import java.util.Vector;

final class VictimTable implements Config {
    private Vector victims = new Vector(); // elements are of type Victim

    private HashMap victimsHash = new HashMap(); // keys are IbisIdentifier, elements are of type Sendport

    // all victims grouped by cluster
    /*
     * clusters are never removed, even though they're empty.
     * (Rob, is that ok??? -maik)
     */
    private Vector clusters = new Vector();

    private Cluster thisCluster;

    private HashMap clustersHash = new HashMap();

    private Satin satin;

    VictimTable(Satin s) {
        this.satin = s;
        thisCluster = new Cluster(s.ident.cluster());
        clusters.add(thisCluster);
        clustersHash.put(s.ident.cluster(), thisCluster);
    }

    void add(IbisIdentifier ident, ReceivePortIdentifier recv, SendPort port) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }
        Victim v = new Victim();
        v.ident = ident;
        v.s = port;
        v.r = recv;
        victims.add(v);
        victimsHash.put(ident, port);

        Cluster c = (Cluster) clustersHash.get(ident.cluster());
        if (c == null) { // new cluster
            c = new Cluster(v); //v is automagically added to this cluster
            clusters.add(c);
            clustersHash.put(ident.cluster(), c);
        } else {
            c.add(v);
        }
    }

    Victim remove(IbisIdentifier ident) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }
        Victim v = new Victim();
        v.ident = ident;

        int i = victims.indexOf(v);

        /*
         * this already happens below if(i < 0) { return null; }
         */

        return remove(i);
    }

    Victim remove(int i) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        // ??? hier een assert van maken??, let op bij 'this already happ...'
        if (i < 0 || i >= victims.size()) {
            return null;
        }

        Victim v = (Victim) victims.remove(i);
        victimsHash.remove(v.ident);

        Cluster c = (Cluster) clustersHash.get(v.ident.cluster());
        c.remove(v);

        return v;
    }

    int size() {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }
        return victims.size();
    }

    SendPort getPort(int i) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }
        if (i < 0 || i >= victims.size()) {
            return null;
        }
        return ((Victim) victims.get(i)).s;
    }

    IbisIdentifier getIdent(int i) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }
        if (i < 0 || i >= victims.size()) {
            return null;
        }
        return ((Victim) victims.get(i)).ident;
    }

    Victim getVictim(int i) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }
        if (i < 0 || i >= victims.size()) {
            return null;
        }
        return (Victim) victims.get(i);
    }

    SendPort getReplyPort(IbisIdentifier ident) {
        return (SendPort) victimsHash.get(ident);
    }

    /*
     * Victim getMasterVictim() {
     *     Victim v = null;
     * 
     *     if (ASSERTS) {
     *         Satin.assertLocked(satin);
     *     }
     * 
     *     try {
     *         v = ((Victim)victims.get(0));
     *     } catch (Exception e) {
     *         commLogger.fatal("Exception in getMasterVictim: " + e, e);
     *         System.exit(1);
     *     }
     * 
     *     if(ASSERTS && v == null) {
     *         commLogger.fatal("getMasterVictim: v == null");
     *         System.exit(1);
     *     }
     * 
     *     return v;
     * }
     */

    Victim getVictim(IbisIdentifier ident) {
        Victim v = null;

        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        for (int i = 0; i < victims.size(); i++) {
            try {
                v = ((Victim) victims.get(i));
            } catch (Exception e) {
                commLogger.debug("Exception in getVictim: " + e, e);
                return null;
            }

            if (v.ident.equals(ident)) {
                return v;
            }
        }

        return null;
    }

    Victim getRandomVictim() {
        Victim v = null;
        int index;

        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        if (victims.size() == 0) {
            // can happen with open world, no others have joined yet.
            return null;
        }

        try {
            index = Math.abs(satin.random.nextInt()) % victims.size();
            v = ((Victim) victims.get(index));
        } catch (Exception e) {
            commLogger.fatal("Exception in getRandomVictim: " + e, e);
            System.exit(1);
        }

        if (ASSERTS && v == null) {
            commLogger.fatal("getRandomVictim: v == null");
            System.exit(1);
        }

        return v;
    }

    /**
     * returns null if there are no other nodes in this cluster
     */
    Victim getRandomLocalVictim() {
        Victim v = null;
        int index;
        int clusterSize = thisCluster.size();

        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        if (clusterSize == 0) {
            return null;
        }

        index = Math.abs(satin.random.nextInt()) % clusterSize;
        v = thisCluster.get(index);

        if (ASSERTS && v == null) {
            commLogger.fatal("getRandomLocalVictim: v == null");
            System.exit(1);
        }

        return v;
    }

    /**
     * Returns null if there are no remote victims i.e., there's only one
     * cluster
     */
    Victim getRandomRemoteVictim() {
        Victim v = null;
        int vIndex, cIndex;
        int remoteVictims;
        Cluster c;

        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        if (ASSERTS && clusters.get(0) != thisCluster) {
            commLogger.fatal("getRandomRemoteVictim: firstCluster != me");
            System.exit(1);
        }

        remoteVictims = victims.size() - thisCluster.size();

        if (remoteVictims == 0) {
            return null;
        }

        vIndex = Math.abs(satin.random.nextInt()) % remoteVictims;

        //find the cluster and index in the cluster for the victim
        cIndex = 1;
        c = (Cluster) clusters.get(cIndex);
        while (vIndex >= c.size()) {
            vIndex -= c.size();
            cIndex += 1;
            c = (Cluster) clusters.get(cIndex);
        }

        v = c.get(vIndex);

        if (ASSERTS && v == null) {
            commLogger.fatal("getRandomRemoteVictim: v == null");
            System.exit(1);
        }

        return v;
    }

    void print(java.io.PrintStream out) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        out.println("victimtable on " + satin + ", size is " + victims.size());

        for (int i = 0; i < victims.size(); i++) {
            out.println("   " + victims.get(i));
        }
    }

    boolean contains(IbisIdentifier ident) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        return victims.contains(ident);
    }
}
