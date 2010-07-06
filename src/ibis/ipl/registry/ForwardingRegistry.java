package ibis.ipl.registry;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.NoSuchPropertyException;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

public final class ForwardingRegistry extends ibis.ipl.registry.Registry {
    
    private final ibis.ipl.registry.Registry target;

    public ForwardingRegistry(ibis.ipl.registry.Registry target) {
        this.target = target;
    }

    @Override
    public long getSequenceNumber(String name) throws IOException {
        return target.getSequenceNumber(name);
    }

    @Override
    public void leave() throws IOException {
        target.leave();
    }

    public void assumeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        target.assumeDead(ibis);
    }

    public ibis.ipl.IbisIdentifier elect(String election, long timeoutMillis) throws IOException {
        return target.elect(election, timeoutMillis);
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
    
    public String getPoolName() {
        return target.getPoolName();
    }

    public boolean isClosed() {
        return target.isClosed();
    }
    
    public void waitUntilPoolClosed() {
        target.waitUntilPoolClosed();
    }

    public Map<String, String> managementProperties() {
        return target.managementProperties();
    }

    public String getManagementProperty(String key) throws NoSuchPropertyException {
        return target.getManagementProperty(key);
    }

    public void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
        target.setManagementProperties(properties);
    }

    public void setManagementProperty(String key, String value) throws NoSuchPropertyException {
        target.setManagementProperty(key, value);
    }

    public void printManagementProperties(PrintStream stream) {
        target.printManagementProperties(stream);
    }

    public boolean hasTerminated() {
        return target.hasTerminated();
    }

    public void terminate() throws IOException {
        target.terminate();
    }

    public IbisIdentifier waitUntilTerminated() {
        return target.waitUntilTerminated();
    }

    @Override
    public ibis.ipl.impl.IbisIdentifier getRandomPoolMember() {
        return target.getRandomPoolMember();
    }

    public String[] wonElections() {
        return target.wonElections();
    }
  

}
