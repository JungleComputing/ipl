package ibis.ipl.impl.messagePassing;

import java.io.IOException;

import ibis.ipl.IbisException;

class Registry implements ibis.ipl.Registry {

    ReceivePortNameServer nameServer;
    ReceivePortNameServerClient nameServerClient;
    ElectionClient electionClient;
    ElectionServer electionServer;

    Registry() throws IbisException, IOException {
	if (ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu == 0) {
	    if (nameServer != null) {
		throw new IbisException("ReceivePortNameServer already initialized");
	    }
	    nameServer = ibis.ipl.impl.messagePassing.Ibis.myIbis.createReceivePortNameServer();
	}
	nameServerClient = ibis.ipl.impl.messagePassing.Ibis.myIbis.createReceivePortNameServerClient();
    }

    void init() throws IbisException {
	if (ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu == 0) {
	    if (electionServer != null) {
		throw new IbisException("ReceivePortNameServer already initialized");
	    }
	    electionServer = new ElectionServer();
	}
	electionClient = new ElectionClient();
    }

    void bind(String name, ibis.ipl.ReceivePortIdentifier id) throws IbisException {
	nameServerClient.bind(name, (ReceivePortIdentifier)id);
    }

    public ibis.ipl.ReceivePortIdentifier lookup(String name) throws IbisException {
	return lookup(name, 0);
    }

    public ibis.ipl.ReceivePortIdentifier lookup(String name, long timeout) throws IbisException {
	return nameServerClient.lookup(name, timeout);
    }

    void unbind(String name) throws IbisException {
	nameServerClient.unbind(name);
    }

    public ibis.ipl.IbisIdentifier locate(String name) throws IbisException {
	/* not implemented yet */
	return locate(name, 0);
    }

    public ibis.ipl.IbisIdentifier locate(String name, long millis) throws IbisException {
	/* not implemented yet */
	return null;
    }

    public ibis.ipl.ReceivePortIdentifier[] query(ibis.ipl.IbisIdentifier ident) throws
	IbisException {
	/* not implemented yet */
	return null;
    }

    public Object elect(String election, Object candidate) throws IbisException {
	return electionClient.elect(election, candidate);
    }
}
