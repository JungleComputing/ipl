package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

import ibis.ipl.impl.messagePassing.ElectionClient;
import ibis.ipl.impl.messagePassing.ElectionServer;

class Registry implements ibis.ipl.Registry {

    ReceivePortNameServer nameServer;
    ReceivePortNameServerClient nameServerClient;
    ElectionClient electionClient;
    ElectionServer electionServer;

    Registry() throws IbisIOException {
	if (ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu == 0) {
	    if (nameServer != null) {
		throw new IbisIOException("ReceivePortNameServer already initialized");
	    }
	    nameServer = ibis.ipl.impl.messagePassing.Ibis.myIbis.createReceivePortNameServer();
	}
	nameServerClient = ibis.ipl.impl.messagePassing.Ibis.myIbis.createReceivePortNameServerClient();
    }

    void init() throws IbisException, IbisIOException {
	if (ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu == 0) {
	    if (electionServer != null) {
		throw new IbisIOException("ReceivePortNameServer already initialized");
	    }
	    electionServer = new ElectionServer();
	}
	electionClient = new ElectionClient();
    }

    void bind(String name, ibis.ipl.ReceivePortIdentifier id) throws IbisIOException {
	nameServerClient.bind(name, (ReceivePortIdentifier)id);
    }

    public ibis.ipl.ReceivePortIdentifier lookup(String name) throws IbisIOException {
	return lookup(name, 0);
    }

    public ibis.ipl.ReceivePortIdentifier lookup(String name, long timeout) throws IbisIOException {
	return nameServerClient.lookup(name, timeout);
    }

    void unbind(String name) throws IbisIOException {
	nameServerClient.unbind(name);
    }

    public ibis.ipl.IbisIdentifier locate(String name) throws IbisIOException {
	/* not implemented yet */
	return locate(name, 0);
    }

    public ibis.ipl.IbisIdentifier locate(String name, long millis) throws IbisIOException {
	/* not implemented yet */
	return null;
    }

    public ibis.ipl.ReceivePortIdentifier[] query(ibis.ipl.IbisIdentifier ident) throws
	IbisIOException {
	/* not implemented yet */
	return null;
    }

    public Object elect(String election, Object candidate) throws IbisIOException {
	return electionClient.elect(election, candidate);
    }
}
