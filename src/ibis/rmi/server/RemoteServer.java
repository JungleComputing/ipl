package ibis.rmi.server;

import ibis.rmi.impl.RTS;

/**
 * <code>RemoteServer</code> is the common superclass for server
 * implementations.
 */

public abstract class RemoteServer extends RemoteObject
{
    /**
     * Constructs a <code>RemoteServer</code>.
     */
    protected RemoteServer() {
	super();
    }
    
    /**
     * Constructs a <code>RemoteServer</code> with the specified reference.
     */
    protected RemoteServer(RemoteRef ref) {
	super(ref);
    }

    /**
     * Returns a string representation of the client host of the
     * remote method invocation currently being processed in the
     * current thread.
     * @return a string representation of the client host
     * @exception ServerNotActiveException if the current thread is not
     *  servicing a remote method invocation
     */
    public static String getClientHost() throws ServerNotActiveException {
	String hostname = RTS.getClientHost();
	if (hostname == null) {
	    throw new ServerNotActiveException();
	}
	return hostname;
    }

/*    public static void setLog(OutputStream out) 
    {
	if (out != null) {
	    log = LogStream.log(log_name);
	    log.setOutputStream(out);
	}
	else	log = null;
    }

    public static PrintStream getLog() 
    {
	return log;
    }*/
}
