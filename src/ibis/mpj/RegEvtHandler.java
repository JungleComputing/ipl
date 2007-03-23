package ibis.mpj;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;

class RegEvtHandler implements RegistryEventHandler {

    IbisIdentifier myId;
    int nInstances;
    int myRank;
    private int joinCount;
    IbisIdentifier[] identifiers;

    public synchronized void joined(IbisIdentifier ident) {
        if (ident.equals(myId)) {
            myRank = joinCount;
        }
        identifiers[joinCount++] = ident;
        if (joinCount == nInstances) {
            notifyAll();
        }
    }

    public void left(IbisIdentifier ident) {
        // ignored
    }

    public void died(IbisIdentifier corpse) {
        // ignored
    }

    public void mustLeave(IbisIdentifier[] ibisses) {
        // ignored
    }

    void waitForEveryone(Ibis ibis) {
        nInstances = ibis.totalNrOfIbisesInPool();
        identifiers = new IbisIdentifier[nInstances];
        myId = ibis.identifier();
        ibis.enableRegistryEvents();
        synchronized(this) {
            while (joinCount < nInstances) {
                try {
                    wait();
                } catch(Exception e) {
                    // ignored
                }
            }
        }
    }
}
