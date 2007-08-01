/*
 * Created on Apr 26, 2006 by rob
 */
package ibis.satin.impl.aborts;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.communication.Protocol;
import ibis.satin.impl.loadBalancing.Victim;
import ibis.satin.impl.spawnSync.InvocationRecord;
import ibis.satin.impl.spawnSync.Stamp;

import java.io.IOException;
import java.util.ArrayList;

final class AbortsCommunication implements Config {

    final class StampListElement {
        Stamp stamp;

        IbisIdentifier stealer;
    }

    final class AbortMessageSender extends Thread {
        AbortMessageSender() {
            setDaemon(true);
            setName("Satin AbortRequestHandler");
        }

        public void run() {
            while (true) {
                StampListElement e = null;
                synchronized (s) {
                    if (stampsToAbortList.size() > 0) {
                        e = stampsToAbortList.remove(0);
                    } else {
                        try {
                            s.wait();
                        } catch (Exception x) {
                            // ignore
                        }
                        continue;
                    }
                }

                soRealSendAbortMessage(e);
            }
        }
    }

    private Satin s;

    private ArrayList<StampListElement> stampsToAbortList =
            new ArrayList<StampListElement>();

    AbortsCommunication(Satin s) {
        this.s = s;
        new AbortMessageSender().start();
    }

    protected void sendAbortMessage(InvocationRecord r) {
        if (s.deadIbises.contains(r.getStealer())) {
            /* don't send abort and store messages to crashed ibises */
            return;
        }

        abortLogger.debug("SATIN '" + s.ident + ": sending abort message to: "
                + r.getStealer() + " for job " + r.getStamp() + ", parent = "
                + r.getParentStamp());

        // TODO use other lock
        synchronized (s) {
            StampListElement e = new StampListElement();
            e.stamp = r.getParentStamp();
            e.stealer = r.getStealer();
            stampsToAbortList.add(e);
            s.notifyAll();
        }
    }

    /*
     * message combining for abort messages does not work (I tried). It is very
     * unlikely that one node stole more than one job from me
     */
    protected void soRealSendAbortMessage(StampListElement e) {
        WriteMessage writeMessage = null;
        try {
            Victim v = null;
            synchronized (s) {
                v = s.victims.getVictim(e.stealer);
                if (v == null)
                    return; // node might have crashed
            }

            writeMessage = v.newMessage();
            writeMessage.writeByte(Protocol.ABORT);
            writeMessage.writeObject(e.stamp);
            long cnt = v.finish(writeMessage);
            if (v.inDifferentCluster(s.ident)) {
                s.stats.interClusterMessages++;
                s.stats.interClusterBytes += cnt;
            } else {
                s.stats.intraClusterMessages++;
                s.stats.intraClusterBytes += cnt;
            }
        } catch (IOException x) {
            if (writeMessage != null) {
                writeMessage.finish(x);
            }
            abortLogger.warn("SATIN '" + s.ident
                    + "': Got Exception while sending abort message (continuing): " + x, x);
            // This should not be a real problem, it is just inefficient.
            // Let's continue...
        }
    }

    protected void handleAbort(ReadMessage m) {
        try {
            Stamp stamp = (Stamp) m.readObject();
            synchronized (s) {
                s.aborts.addToAbortList(stamp);
            }
            // m.finish();
        } catch (IOException e) {
            abortLogger.error("SATIN '" + s.ident
                    + "': got exception while reading job result: " + e, e);
        } catch (ClassNotFoundException e1) {
            abortLogger.error("SATIN '" + s.ident
                    + "': got exception while reading job result: " + e1, e1);
        }
    }
}
