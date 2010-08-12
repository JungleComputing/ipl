package ibis.ipl.impl.stacking.sns;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.Registry;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

public class SNSRegistry implements Registry{
	
	Registry base;
	SNSIbis snsIbis;
	
	public SNSRegistry(SNSIbis snsIbis){
		this.snsIbis = snsIbis;
		base = snsIbis.mIbis.registry();
		
	}
	
	@Override
	public void assumeDead(IbisIdentifier ibisIdentifier) throws IOException {
		base.assumeDead(ibisIdentifier);	
	}

	@Override
	public IbisIdentifier[] diedIbises() {
		return base.diedIbises();
	}

	@Override
	public void disableEvents() {
		base.disableEvents();		
	}

	@Override
	public IbisIdentifier elect(String electionName) throws IOException {
		IbisIdentifier result =  base.elect(electionName);
		snsIbis.allowedIbisIdent.add(result);
		return result;
	}

	@Override
	public IbisIdentifier elect(String electionName, long timeoutMillis)
			throws IOException {
		IbisIdentifier result =  base.elect(electionName, timeoutMillis);
		snsIbis.allowedIbisIdent.add(result);
		return result;
	}

	@Override
	public void enableEvents() {
		base.enableEvents();		
	}

	@Override
	public IbisIdentifier getElectionResult(String electionName)
			throws IOException {
		IbisIdentifier result = base.getElectionResult(electionName);
		snsIbis.allowedIbisIdent.add(result);
		return result;
	}

	@Override
	public IbisIdentifier getElectionResult(String electionName,
			long timeoutMillis) throws IOException {
		IbisIdentifier result = base.getElectionResult(electionName, timeoutMillis); 
		snsIbis.allowedIbisIdent.add(result);
		return result;
	}

	@Override
	public String getPoolName() {
		return base.getPoolName();
	}

	@Override
	public int getPoolSize() {
		return base.getPoolSize();
	}

	@Override
	public long getSequenceNumber(String name) throws IOException {
		return base.getSequenceNumber(name);
	}

	@Override
	public boolean hasTerminated() {
		return base.hasTerminated();
	}

	@Override
	public boolean isClosed() {
		return base.isClosed();
	}

	@Override
	public IbisIdentifier[] joinedIbises() {
		return base.joinedIbises();
	}

	@Override
	public IbisIdentifier[] leftIbises() {
		return base.leftIbises();
	}

	@Override
	public void maybeDead(IbisIdentifier ibisIdentifier) throws IOException {
		base.maybeDead(ibisIdentifier);
	}

	@Override
	public String[] receivedSignals() {
		return base.receivedSignals();
	}

	@Override
	public void signal(String signal, IbisIdentifier... ibisIdentifiers)
			throws IOException {
		base.signal(signal, ibisIdentifiers);	
	}

	@Override
	public void terminate() throws IOException {
		base.terminate();
	}

	@Override
	public void waitUntilPoolClosed() {
		base.waitUntilPoolClosed();
	}

	@Override
	public IbisIdentifier waitUntilTerminated() {
		return base.waitUntilTerminated();
	}

	@Override
	public String getManagementProperty(String key)
			throws NoSuchPropertyException {
		return base.getManagementProperty(key);
	}

	@Override
	public Map<String, String> managementProperties() {
		return base.managementProperties();
	}

	@Override
	public void printManagementProperties(PrintStream stream) {
		base.printManagementProperties(stream);
	}

	@Override
	public void setManagementProperties(Map<String, String> properties)
			throws NoSuchPropertyException {
		base.setManagementProperties(properties);	
	}

	@Override
	public void setManagementProperty(String key, String value)
			throws NoSuchPropertyException {
		base.setManagementProperty(key, value);
	}

}
