package ibis.impl.nio;

import ibis.impl.nameServer.NameServer;
import ibis.ipl.Ibis;
import ibis.ipl.IbisException;
import ibis.ipl.IbisRuntimeException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.StaticProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

public final class NioIbis extends Ibis implements Config {

    private NioIbisIdentifier ident;

    NameServer nameServer;
    private int poolSize;

    private Hashtable portTypeList = new Hashtable();

    private boolean open = false;
    private ArrayList joinedIbises = new ArrayList();
    private ArrayList leftIbises = new ArrayList();

    private final StaticProperties systemProperties = new StaticProperties();	
    NioChannelFactory factory;
    private boolean ended = false;

    public NioIbis() throws IbisException {
	// Set my properties.
	systemProperties.add("reliability", "true");
	systemProperties.add("multicast", "true") ;
	systemProperties.add("totally ordered multicast", "false") ;

	try {
	    Runtime.getRuntime().addShutdownHook(new NioShutdown());
	} catch (Exception e) {
	    System.err.println("Warning: could not register nio shutdown hook");
	}
    }

    protected PortType newPortType(String name, StaticProperties p)
	throws IOException, IbisException {

	    NioPortType resultPort = new NioPortType(this, name, p);
	    p = resultPort.properties();

	    if (nameServer.newPortType(name, p)) { 
		/* add type to our table */
		portTypeList.put(name, resultPort);

		if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
		    System.out.println(this.name + ": created PortType '" + name + "'");
		}
	    }

	    return resultPort;
	}

    public Registry registry() {
	return nameServer;
    } 

    public StaticProperties properties() { 
	return systemProperties;
    }

    public IbisIdentifier identifier() {
	return ident;
    }

    public String toString() {
	return ident.toString();
    }

    protected void init() throws IbisException, IOException { 
	if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
	    System.err.println("Initializing NioIbis");
	}
	poolSize = 1;

	ident = new NioIbisIdentifier(name);

	if(DEBUG_LEVEL >= MEDIUM_DEBUG_LEVEL) {
	    System.err.println("Created IbisIdentifier " + ident);
	}

	nameServer = NameServer.loadNameServer(this);

	factory = new TcpNioChannelFactory();
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("NioIbis initialization compleet");
	}
    }

    /**
     * this method forwards the join to the application running on top of ibis.
     */
    public void join(IbisIdentifier joinIdent) { 
	synchronized (this) {
	    if(!open && resizeHandler != null) {
		joinedIbises.add(joinIdent);
		return;
	    }

	    //FIXME : what if !open && reziseHandler == null ??

	    if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
		System.out.println(name + ": Ibis '" + joinIdent.name() + "' joined"); 
	    }

	    poolSize++;
	}

	if(resizeHandler != null) {
	    resizeHandler.join(joinIdent);
	}
    }

    /**
     * this method forwards the leave to the application running on top of
     * ibis.
     */
    public void leave(IbisIdentifier leaveIdent) { 
	synchronized (this) {
	    if(!open && resizeHandler != null) {
		leftIbises.add(leaveIdent);
		return;
	    }


	    if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
		System.out.println(name + ": Ibis '" + leaveIdent.name() + "' left"); 
	    }
	    poolSize--;
	}

	if(resizeHandler != null) {
	    resizeHandler.leave(leaveIdent);
	}
    }

    public PortType getPortType(String name) { 
	return (PortType) portTypeList.get(name);
    } 

    public void openWorld() {
	NioIbisIdentifier ident = null;

	if(resizeHandler != null) {
	    while(true) {
		synchronized(this) {
		    if(joinedIbises.size() == 0) break;
		    poolSize++;
		    ident = (NioIbisIdentifier)joinedIbises.remove(0);
		}
		resizeHandler.join(ident); // Don't hold the lock during user upcall
	    }

	    while(true) {
		synchronized(this) {
		    if(leftIbises.size() == 0) break;
		    poolSize--;
		    ident = (NioIbisIdentifier)leftIbises.remove(0);
		}
		resizeHandler.leave(ident); // Don't hold the lock during user upcall

	    }
	}

	synchronized (this) {
	    open = true;
	}

	if(DEBUG_LEVEL >= MEDIUM_DEBUG_LEVEL) {
	    System.out.println(name + ": Ibis world open"); 
	}
    }

    public synchronized void closeWorld() {
	open = false;
    }

    public void end() {
	synchronized(this) {
	    if(ended) {
		return;
	    }
	    ended = true;
	}
	try { 
	    if(nameServer != null) {
		nameServer.leave();
	    }
	    if(factory != null) {
		factory.quit();
	    }
	} catch (Exception e) { 
	    throw new IbisRuntimeException("NioIbis: end failed ", e);
	} 
    }

    /**
     * does nothing.
     */
    public void poll() throws IOException {
    }

    /**
     * Called when the vm exits
     */
    class NioShutdown extends Thread {
	public void run() {
	    end();
	}
    }

}
