package ibis.impl.messagePassing;

import ibis.ipl.IbisException;

import java.io.IOException;

/**
 * messagePassing implementation of Registry
 */
class Registry implements ibis.ipl.Registry {

    private ReceivePortNameServer nameServer;
    private ReceivePortNameServerClient nameServerClient;
    private ElectionClient electionClient;
    private ElectionServer electionServer;

    private final static boolean EXPORT_ELECT = true;


    Registry() throws IOException {
	if (Ibis.myIbis.myCpu == 0) {
	    nameServer = Ibis.myIbis.createReceivePortNameServer();
	}
	nameServerClient = Ibis.myIbis.createReceivePortNameServerClient();
    }


    void init() throws IbisException, IOException {
	if (EXPORT_ELECT) {
	    if (Ibis.myIbis.myCpu == 0) {
		if (electionServer != null) {
		    throw new IbisException("ReceivePortNameServer already initialized");
		}
		electionServer = new ElectionServer();
	    }
	    electionClient = new ElectionClient();
	}
    }


    void end() throws IOException {
	if (EXPORT_ELECT) {
	    if (electionServer != null) {
		electionServer.end();
	    }
	    if (electionClient != null) {
		electionClient.end();
	    }
	    if (electionServer != null) {
		electionServer.awaitShutdown();
	    }
	}
    }


    public void bind(String name, ibis.ipl.ReceivePortIdentifier id) throws IOException {
	nameServerClient.bind(name, (ReceivePortIdentifier)id);
    }


    public void rebind(String name, ibis.ipl.ReceivePortIdentifier id) throws IOException {
	nameServerClient.unbind(name);
	nameServerClient.bind(name, (ReceivePortIdentifier)id);
    }


    public ibis.ipl.ReceivePortIdentifier lookupReceivePort(String name) throws IOException {
	return lookupReceivePort(name, 0);
    }


    public ibis.ipl.ReceivePortIdentifier lookupReceivePort(String name, long timeout) throws IOException {
	return nameServerClient.lookup(name, timeout);
    }


    public void unbind(String name) throws IOException {
	nameServerClient.unbind(name);
    }


    public ibis.ipl.IbisIdentifier lookupIbis(String name) throws IOException {
	/* not implemented yet */
	return lookupIbis(name, 0);
    }


    public ibis.ipl.IbisIdentifier lookupIbis(String name, long millis) throws IOException {
	/* not implemented yet */
	return null;
    }

    public boolean isDead(ibis.ipl.IbisIdentifier id) throws IOException {
	/* not implemented yet */
	return false;
    }

    public void dead(ibis.ipl.IbisIdentifier id) throws IOException {
	/* not implemented yet */
    }

    public String[] listNames(String pattern) throws IOException {
	/* not implemented yet */
	return null;
    }

    public ibis.ipl.ReceivePortIdentifier[] listReceivePorts(ibis.ipl.IbisIdentifier ident)
	    throws IOException {
	/* not implemented yet */
	return null;
    }


    public ibis.ipl.IbisIdentifier  elect(String election) throws IOException {
	if (EXPORT_ELECT) {
	    return (ibis.ipl.IbisIdentifier) electionClient.elect(election, Ibis.myIbis.identifier());
	}
	throw new IOException("Registry.elect not implemented");
    }

    public ibis.ipl.IbisIdentifier  getElectionResult(String election) throws IOException {
	if (EXPORT_ELECT) {
	    return (ibis.ipl.IbisIdentifier) electionClient.elect(election, null);
	}
	throw new IOException("Registry.elect not implemented");
    }
}
