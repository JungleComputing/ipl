package ibis.rmi.server;

import ibis.rmi.RTS;

public abstract class RemoteServer extends RemoteObject
{
    protected RemoteServer() {
	super();
    }
    
    protected RemoteServer(RemoteRef ref) {
	super(ref);
    }

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
