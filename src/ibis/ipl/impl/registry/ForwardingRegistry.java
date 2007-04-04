package ibis.ipl.impl.registry;

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

    public void dead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        target.dead(ibis);
    }

    public ibis.ipl.IbisIdentifier elect(String election) throws IOException {
        return target.elect(election);
    }

    public ibis.ipl.IbisIdentifier getElectionResult(String election) throws IOException {
        return target.getElectionResult(election);
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

}
