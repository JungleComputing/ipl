/*
 * Created on Jun 1, 2006
 */
package ibis.satin.impl.faultTolerance;

import java.util.ArrayList;

import ibis.ipl.IbisIdentifier;
import ibis.satin.impl.Satin;

class JoinThread extends Thread {
    private Satin s;

    private ArrayList joiners = new ArrayList();

    JoinThread(Satin s) {
        this.s = s;
        setDaemon(true);
    }

    public void run() {
        while (true) {
            IbisIdentifier[] j = null;
            synchronized (this) {
                if (joiners.size() != 0) {
                    j = (IbisIdentifier[]) joiners
                        .toArray(new IbisIdentifier[0]);
                    joiners.clear();
                }
            }
            if (j != null && j.length != 0) {
                s.ft.ftComm.handleJoins(j);
                synchronized (this) {
                    notifyAll();
                }
            }

            // Sleep 1 second, maybe more nodes have joined within this second.
            // we can aggregate the port lookups for all those joiners.
            try {
                //                synchronized (this) {
                //                    wait();
                //                }
                Thread.sleep(1000);
            } catch (Exception e) {
                // ignored
            }
        }
    }

    protected synchronized void waitForEarlierJoins() {
        while (joiners.size() != 0) {
            try {
                wait();
            } catch (Exception e) {
                // ignored
            }
        }
    }

    protected synchronized void addJoiner(IbisIdentifier joiner) {
        joiners.add(joiner);
        notifyAll();
    }
}
