/* $Id$ */

package ibis.satin.impl.spawnSync;

import ibis.ipl.IbisIdentifier;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.loadBalancing.Victim;

import java.util.ArrayList;

/** A stack of invocation records. */

public final class IRStack implements Config {
    private InvocationRecord[] l = new InvocationRecord[500];

    private int count = 0;

    private Satin s;

    public IRStack(Satin s) {
        this.s = s;
    }

    public int size() {
        return count;
    }

    public boolean contains(InvocationRecord r) {
        InvocationRecord curr;

        for (int i = 0; i < count; i++) {
            curr = l[i];
            if (curr.equals(r)) {
                return true;
            }
        }

        return false;
    }

    public void push(InvocationRecord r) {
        if (count >= l.length) {
            InvocationRecord[] nl = new InvocationRecord[l.length * 2];
            System.arraycopy(l, 0, nl, 0, l.length);
            l = nl;
        }

        l[count] = r;
        count++;
    }

    public void pop() {
        if (ASSERTS && count <= 0) {
            System.err.println("popping from empty IR stack!");
            new Exception().printStackTrace();
            System.exit(1); // Failed assertion
        }

        count--;
        l[count] = null;
    }

    /**
     * If store is true (used for fault tolerance), a list of jobs to store
     * is returned. Otherwise, null is returned
     */
    public ArrayList<InvocationRecord> killChildrenOf(Stamp targetStamp, boolean store) {
        Satin.assertLocked(s);
        ArrayList<InvocationRecord> toStore = null;
        if (store) {
            toStore = new ArrayList<InvocationRecord>();
        }

        for (int i = 0; i < count; i++) {
            InvocationRecord curr = l[i];
            if (curr.aborted) {
                continue; // already handled.
            }

            if ((curr.getParent() != null && curr.getParent().aborted)
                || curr.isDescendentOf(targetStamp)) {
                curr.aborted = true;

                if (store) {
                    s.stats.killedOrphans++;
                    toStore.add(curr);
                } else {
                    s.stats.abortedJobs++;
                }

                if (false && abortLogger.isDebugEnabled()) {
                    abortLogger.debug("found child on the stack: "
                        + curr.getStamp() + ", it depends on " + targetStamp);
                }
            }
        }

        return toStore;
    }

    /**
     * Used for fault tolerance. Kill every job that was spawned on targetOwner
     * or is a child of a job spawned on targetOwner. Store all finished children
     * of the aborted jobs in the global result table.
     */
    public ArrayList<InvocationRecord> killSubtreesOf(IbisIdentifier targetOwner) {
        Satin.assertLocked(s);
        ArrayList<InvocationRecord> toStore = new ArrayList<InvocationRecord>();
        for (int i = 0; i < count; i++) {
            InvocationRecord curr = l[i];

            if (curr.aborted) continue; // already handled

            if ((curr.getParent() != null && curr.getParent().aborted)
                || curr.isDescendentOf(targetOwner)
                || curr.getOwner().equals(targetOwner)) {

                curr.aborted = true;
                s.stats.killedOrphans++;
                toStore.add(curr);
            }
        }
        return toStore;
    }

    public ArrayList<InvocationRecord> getAllFinishedChildren(Victim victim) {
        Satin.assertLocked(s);
        InvocationRecord curr, child;
        ArrayList<InvocationRecord> toPush = new ArrayList<InvocationRecord>();

        for (int i = 0; i < count; i++) {
            curr = l[i];
            child = curr.getFinishedChild();
            while (child != null) {
                toPush.add(child);
                child = child.getFinishedSibling();
            }
        }
        return toPush;
    }

    public void print(java.io.PrintStream out) {
        out.println("=IRStack " + s.ident + ":=============");
        for (int i = 0; i < count; i++) {

            out.println("stack [" + i + "] = " + l[i]);
        }
        out.println("=========end of IRStack:===========");
    }
}
