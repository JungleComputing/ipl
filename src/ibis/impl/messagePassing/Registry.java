package ibis.impl.messagePassing;

import java.io.IOException;

import ibis.ipl.IbisException;

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


    public ibis.ipl.ReceivePortIdentifier lookup(String name) throws IOException {
	return lookup(name, 0);
    }


    public ibis.ipl.ReceivePortIdentifier lookup(String name, long timeout) throws IOException {
	return nameServerClient.lookup(name, timeout);
    }


    public void unbind(String name) throws IOException {
	nameServerClient.unbind(name);
    }


    public ibis.ipl.IbisIdentifier locate(String name) throws IOException {
	/* not implemented yet */
	return locate(name, 0);
    }


    public ibis.ipl.IbisIdentifier locate(String name, long millis) throws IOException {
	/* not implemented yet */
	return null;
    }


    public String[] list(String pattern) throws IOException {
	/* not implemented yet */
	return null;
    }

    public ibis.ipl.ReceivePortIdentifier[] query(ibis.ipl.IbisIdentifier ident)
	    throws IOException {
	/* not implemented yet */
	return null;
    }


    public Object elect(String election, Object candidate) throws IOException {
	if (EXPORT_ELECT) {
	    return electionClient.elect(election, candidate);
	}
	throw new IOException("Registry.elect not implemented");
    }

    public Object reelect(String election, Object candidate, Object formerRuler) throws IOException {
	if (EXPORT_ELECT) {
	    return electionClient.reelect(election, candidate, formerRuler);
	}
	throw new IOException("Registry.elect not implemented");
    }

    
}
