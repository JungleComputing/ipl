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
	handler.died(corpse);
	ibis.died(corpse);
    }

    public void electionResult(String electionName, IbisIdentifier winner) {
	handler.electionResult(electionName, winner);
    }

    public void gotSignal(String signal, IbisIdentifier source) {
	handler.gotSignal(signal, source);
    }

    public void joined(IbisIdentifier joinedIbis) {
	handler.joined(joinedIbis);
    }

    public void left(IbisIdentifier leftIbis) {
	handler.left(leftIbis);
	ibis.left(leftIbis);
    }

    public void poolClosed() {
	handler.poolClosed();
    }

    public void poolTerminated(IbisIdentifier source) {
	handler.poolTerminated(source);
    }

}
