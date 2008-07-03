package ibis.ipl.impl.multi;


import java.io.IOException;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;

public class MultiRegistryEventHandler implements RegistryEventHandler {

    private final RegistryEventHandler subHandler;

    private final MultiIbis ibis;
    private String ibisName;

    public MultiRegistryEventHandler(MultiIbis ibis, RegistryEventHandler subHandler) {
        this.ibis = ibis;
        this.subHandler = subHandler;
    }

    public synchronized void died(IbisIdentifier corpse) {
        while (ibisName == null) {
            try {
                wait();
            } catch (InterruptedException e) {
               // Ignored
            }
        }
        try {
            subHandler.died(ibis.mapIdentifier(corpse, ibisName));
        } catch (IOException e) {
            // TODO What the hell to do.
        }
    }

    public synchronized void electionResult(String electionName, IbisIdentifier winner) {
        while (ibisName == null) {
            try {
                wait();
            } catch (InterruptedException e) {
               // Ignored
            }
        }
        try {
            subHandler.electionResult(electionName, ibis.mapIdentifier(winner, ibisName));
        } catch (IOException e) {
            // TODO What the hell to do
        }
    }

    public void gotSignal(String signal) {
        subHandler.gotSignal(signal);
    }

    public synchronized void joined(IbisIdentifier joinedIbis) {
        while (ibisName == null) {
            try {
                wait();
            } catch (InterruptedException e) {
               // Ignored
            }
        }
        try {
            subHandler.joined(ibis.mapIdentifier(joinedIbis, ibisName));
        } catch (IOException e) {
            // TODO What the hell to do here?
        }
    }

    public synchronized void left(IbisIdentifier leftIbis) {
        while (ibisName == null) {
            try {
                wait();
            } catch (InterruptedException e) {
               // Ignored
            }
        }
        try {
            subHandler.left(ibis.mapIdentifier(leftIbis, ibisName));
        } catch (IOException e) {
            // TODO What the hell to do here?
        }
    }

    public synchronized void setName(String ibisName) {
        this.ibisName = ibisName;
        notifyAll();
    }

}
