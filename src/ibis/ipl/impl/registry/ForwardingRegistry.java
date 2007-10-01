package ibis.ipl.impl.registry;

import ibis.ipl.IbisIdentifier;

import java.io.IOException;

public final class ForwardingRegistry extends ibis.ipl.impl.Registry {
    
    private final ibis.ipl.impl.Registry target;

    public ForwardingRegistry(ibis.ipl.impl.Registry target) {
        this.target = target;
    }

    @Override
    public long getSeqno(String name) throws IOException {
        return target.getSeqno(name);
    }

    @Override
    public void leave() throws IOException {
        target.leave();
    }

    public void assumeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        target.assumeDead(ibis);
    }

    public ibis.ipl.IbisIdentifier elect(String election) throws IOException {
        return target.elect(election);
    }

    public ibis.ipl.IbisIdentifier getElectionResult(String election) throws IOException {
        return target.getElectionResult(election);
    }

    public ibis.ipl.IbisIdentifier getElectionResult(String election, long timeoutMillis) throws IOException {
        return target.getElectionResult(election, timeoutMillis);
    }

    public void maybeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        target.maybeDead(ibis);
    }

    public void signal(String string, ibis.ipl.IbisIdentifier... ibisses) throws IOException {
        target.signal(string, ibisses);
    }

    @Override
    public ibis.ipl.impl.IbisIdentifier getIbisIdentifier() {
        return target.getIbisIdentifier();
    }

    public IbisIdentifier[] diedIbises() {
        return target.diedIbises();
    }

    public IbisIdentifier[] joinedIbises() {
        return target.joinedIbises();
    }

    public IbisIdentifier[] leftIbises() {
        return target.leftIbises();
    }

    public String[] receivedSignals() {
        return target.receivedSignals();
    }

    public void disableEvents() {
        target.disableEvents();
    }

    public void enableEvents() {
        target.enableEvents();
    }

    public int getPoolSize() {
        return target.getPoolSize();
    }

    public void waitUntilPoolClosed() {
        target.waitUntilPoolClosed();
    }

}
