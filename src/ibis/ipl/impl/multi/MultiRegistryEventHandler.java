package ibis.ipl.impl.multi;


import java.io.IOException;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;

public class MultiRegistryEventHandler implements RegistryEventHandler {

    private final RegistryEventHandler subHandler;

    private final MultiIbis ibis;
    private MultiRegistry registry;
    private String ibisName;

    public MultiRegistryEventHandler(MultiIbis ibis, RegistryEventHandler subHandler) {
        this.ibis = ibis;
        this.subHandler = subHandler;
    }

    public synchronized void died(IbisIdentifier corpse) {
        while (registry == null) {
            try {
                wait();
            } catch (InterruptedException e) {
               // Ignored
            }
        }
        try {
            MultiIbisIdentifier id = ibis.mapIdentifier(corpse, ibisName);
            if (!registry.died.containsKey(id)) {
                registry.died.put(id, id);
                subHandler.died(id);
            }
        } catch (IOException e) {
            // TODO What the hell to do.
        }
    }

    public synchronized void electionResult(String electionName, IbisIdentifier winner) {
        while (registry == null) {
            try {
                wait();
            } catch (InterruptedException e) {
               // Ignored
            }
        }
        try {
            MultiIbisIdentifier id = ibis.mapIdentifier(winner, ibisName);
            if (!registry.elected.containsKey(electionName)) {
                registry.elected.put(electionName, id);
                subHandler.electionResult(electionName, id);
            }
            else {
                MultiIbisIdentifier oldWinner = registry.elected.get(electionName);
                if (!oldWinner.equals(id)) {
                    registry.elected.put(electionName, id);
                    subHandler.electionResult(electionName, id);
                }
            }
        } catch (IOException e) {
            // TODO What the hell to do
        }
    }

    public void gotSignal(String signal) {
        subHandler.gotSignal(signal);
    }

    public synchronized void joined(IbisIdentifier joinedIbis) {
        while (registry == null) {
            try {
                wait();
            } catch (InterruptedException e) {
               // Ignored
            }
        }
        try {
            MultiIbisIdentifier id = ibis.mapIdentifier(joinedIbis, ibisName);
            if (!registry.joined.containsKey(id)) {
                registry.joined.put(id, id);
                subHandler.joined(id);
            }
        } catch (IOException e) {
            // TODO What the hell to do here?
        }
    }

    public synchronized void left(IbisIdentifier leftIbis) {
        while (registry == null) {
            try {
                wait();
            } catch (InterruptedException e) {
               // Ignored
            }
        }
        try {
            MultiIbisIdentifier id = ibis.mapIdentifier(leftIbis, ibisName);
            if (!registry.left.containsKey(id)) {
                registry.left.put(id, id);
                subHandler.left(id);
            }
        } catch (IOException e) {
            // TODO What the hell to do here?
        }
    }

    public synchronized void setName(String ibisName) {
        this.ibisName = ibisName;
    }

    public synchronized void setRegistry(MultiRegistry registry) {
        this.registry = (MultiRegistry)ibis.registry();
        notifyAll();
    }

}
