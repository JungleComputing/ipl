/* $Id$ */

package ibis.satin.impl.loadBalancing;

import ibis.ipl.IbisIdentifier;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

public final class VictimTable implements Config {
    private Random random = new Random();

    private Vector victims = new Vector(); // elements are of type Victim

    // all victims grouped by cluster
    private Vector clusters = new Vector();

    private Cluster thisCluster;

    private HashMap clustersHash = new HashMap();

    private Satin satin;

    public VictimTable(Satin s) {
        this.satin = s;
        thisCluster = new Cluster(s.ident.cluster());
        clusters.add(thisCluster);
        clustersHash.put(s.ident.cluster(), thisCluster);
    }

    public void add(Victim v) {
        Satin.assertLocked(satin);
        victims.add(v);

        Cluster c = (Cluster) clustersHash.get(v.getIdent().cluster());
        if (c == null) { // new cluster
            c = new Cluster(v); //v is automagically added to this cluster
            clusters.add(c);
            clustersHash.put(v.getIdent().cluster(), c);
        } else {
            c.add(v);
        }
    }

    public Victim remove(IbisIdentifier ident) {
        Satin.assertLocked(satin);
        Victim v = new Victim(ident, null, null);

        int i = victims.indexOf(v);
        return remove(i);
    }

    public Victim remove(int i) {
        Satin.assertLocked(satin);

        if (i < 0 || i >= victims.size()) {
            return null;
        }

        Victim v = (Victim) victims.remove(i);

        Cluster c = (Cluster) clustersHash.get(v.getIdent().cluster());
        c.remove(v);

        if (c.size() == 0) {
            clustersHash.remove(c);
        }

        return v;
    }

    public int size() {
        Satin.assertLocked(satin);
        return victims.size();
    }

    public Victim getVictim(int i) {
        Satin.assertLocked(satin);
        if (ASSERTS && i < 0 || i >= victims.size()) {
            commLogger.warn("trying to read a non-existing victim id");
            return null;
        }
        return (Victim) victims.get(i);
    }

    private Victim getVictimNonBlocking(IbisIdentifier ident) {
        Satin.assertLocked(satin);
        Victim v = null;
        for (int i = 0; i < victims.size(); i++) {
            v = ((Victim) victims.get(i));

            if (v.getIdent().equals(ident)) {
                return v;
            }
        }

        return null;
    }

    public Victim getRandomVictim() {
        Victim v = null;
        int index;

        Satin.assertLocked(satin);

        if (victims.size() == 0) {
            // can happen with open world, no others have joined yet.
            return null;
        }

        index = Math.abs(random.nextInt()) % victims.size();
        v = ((Victim) victims.get(index));

        return v;
    }

    /**
     * returns null if there are no other nodes in this cluster
     */
    public Victim getRandomLocalVictim() {
        Victim v = null;
        int index;
        int clusterSize = thisCluster.size();

        Satin.assertLocked(satin);

        if (clusterSize == 0) {
            return null;
        }

        index = Math.abs(random.nextInt()) % clusterSize;
        v = thisCluster.get(index);

        return v;
    }

    /**
     * Returns null if there are no remote victims i.e., there's only one
     * cluster
     */
    public Victim getRandomRemoteVictim() {
        Victim v = null;
        int vIndex, cIndex;
        int remoteVictims;
        Cluster c;

        Satin.assertLocked(satin);

        if (ASSERTS && clusters.get(0) != thisCluster) {
            commLogger.fatal("getRandomRemoteVictim: firstCluster != me");
            System.exit(1); // Failed assertion
        }

        remoteVictims = victims.size() - thisCluster.size();

        if (remoteVictims == 0) {
            return null;
        }

        vIndex = Math.abs(random.nextInt()) % remoteVictims;

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

    public void print(java.io.PrintStream out) {
        Satin.assertLocked(satin);

        out.println("victimtable on " + satin + ", size is " + victims.size());

        for (int i = 0; i < victims.size(); i++) {
            out.println("   " + victims.get(i));
        }
    }

    public boolean contains(IbisIdentifier ident) {
        Satin.assertLocked(satin);
        return victims.contains(ident);
    }

    public IbisIdentifier[] getIbises() {
        Satin.assertLocked(satin);
        IbisIdentifier[] tmp = new IbisIdentifier[victims.size()];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = ((Victim) victims.get(i)).getIdent();
        }

        return tmp;
    }

    // retry a couple of times, than assume a crash
    public Victim getVictim(IbisIdentifier id) {
        Satin.assertLocked(satin);
        Victim v = null;

        long start = System.currentTimeMillis();

        do {
            v = getVictimNonBlocking(id);
            if (v != null) return v;

            if (commLogger.isDebugEnabled()) {
                commLogger.debug("SATIN '" + satin.ident
                    + "': could not get reply port to " + id + ", retrying");
            }

            try {
                satin.wait(100);
            } catch (Exception e) {
                // Ignore.
            }
        } while (System.currentTimeMillis() - start < 10000);

        ftLogger.info("SATIN '" + satin.ident + "': could not get victim for "
            + id);

        try {
            satin.comm.ibis.registry().maybeDead(id);
        } catch (IOException e) {
            ftLogger.warn("SATIN '" + satin.ident
                + "': got exception in maybeDead", e);
        }

        return null;
    }
}
