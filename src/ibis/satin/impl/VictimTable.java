/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;

import java.util.HashMap;
import java.util.Vector;

final class VictimTable implements Config {
    private Vector victims = new Vector(); // elements are of type Victim

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

    void add(Victim v) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }
        victims.add(v);

        Cluster c = (Cluster) clustersHash.get(v.ident.cluster());
        if (c == null) { // new cluster
            c = new Cluster(v); //v is automagically added to this cluster
            clusters.add(c);
            clustersHash.put(v.ident.cluster(), c);
        } else {
            c.add(v);
        }
    }

    Victim remove(IbisIdentifier ident) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }
        Victim v = new Victim(ident, null, null);

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

    Victim getVictim(int i) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }
        if (i < 0 || i >= victims.size()) {
            return null;
        }
        return (Victim) victims.get(i);
    }

    Victim getVictim(IbisIdentifier ident) {
        Victim v = null;

        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        for (int i = 0; i < victims.size(); i++) {
            v = ((Victim) victims.get(i));

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

        index = Math.abs(satin.random.nextInt()) % victims.size();
        v = ((Victim) victims.get(index));

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
            System.exit(1);     // Failed assertion
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
