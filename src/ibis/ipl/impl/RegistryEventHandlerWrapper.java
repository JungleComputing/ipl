package ibis.ipl.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;

public class RegistryEventHandlerWrapper implements RegistryEventHandler {
    
    private final RegistryEventHandler handler;
    private final Ibis ibis;
    
    public RegistryEventHandlerWrapper(RegistryEventHandler h, Ibis i) {
	handler = h;
	ibis = i;
    }

    public void died(IbisIdentifier corpse) {
	if (handler != null) {
	    handler.died(corpse);
	}
	ibis.died(corpse);
    }

    public void electionResult(String electionName, IbisIdentifier winner) {
	if (handler != null) {
	    handler.electionResult(electionName, winner);
	}
    }

    public void gotSignal(String signal, IbisIdentifier source) {
	if (handler != null) {
	    handler.gotSignal(signal, source);
	}
    }

    public void joined(IbisIdentifier joinedIbis) {
	if (handler != null) {
	    handler.joined(joinedIbis);
	}
    }

    public void left(IbisIdentifier leftIbis) {
	if (handler != null) {
	    handler.left(leftIbis);
	}
	ibis.left(leftIbis);
    }

    public void poolClosed() {
	if (handler != null) {
	    handler.poolClosed();
	}
    }

    public void poolTerminated(IbisIdentifier source) {
	if (handler != null) {
	    handler.poolTerminated(source);
	}
    }
}
