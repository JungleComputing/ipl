package ibis.ipl.impl.registry;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;

import java.io.DataOutputStream;
import java.io.IOException;

public interface Pool {

	public boolean ended();

	public String getName();

	public IbisIdentifier join(byte[] implementationData, byte[] clientAddress,
			Location location) throws Exception;

	public void leave(IbisIdentifier identifier) throws Exception;

	public void dead(IbisIdentifier identifier);

	public IbisIdentifier elect(String election, IbisIdentifier candidate);

	public void maybeDead(IbisIdentifier identifier);

	public void signal(String signal, IbisIdentifier[] victims);

	public long getSequenceNumber();

	public void writeBootstrap(DataOutputStream out) throws IOException;

}