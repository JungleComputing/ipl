package ibis.connect.routedMessages;

import java.util.Properties;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ibis.connect.util.MyDebug;

public class HubLinkFactory {
    private static HubLink hub = null;
    private static boolean closed = false;
    private static boolean enabled = false;

    static {
	try {
	    Properties p = System.getProperties();
	    String host       = p.getProperty("ibis.connect.hub_host");
	    String portString = p.getProperty("ibis.connect.hub_port");
	    if(host != null && portString != null){
		int port = Integer.parseInt(portString);
		if(MyDebug.VERBOSE())
		    System.err.println("# Creating link to hub- host="+host+"; port="+port);
		hub = new HubLink(host, port);
		hub.start();
		enabled = true;
	    } else if(MyDebug.VERBOSE()) {
		System.err.println("# HubLinkFactory: 'hub' properties not set");
		System.err.println("# (ibis.connect.hub_host, ibis.connect.hub_port).");
		System.err.println("# Not creating wire to hub.");
	    }
	} catch(Exception e) {
	    System.err.println("# HubLinkFactory: Cannot create wire to hub.");
	}
    }

    public synchronized static boolean isConnected() {
	return enabled;
    }

    public synchronized static void destroyHubLink() {
	if(enabled) {
	    hub.stopHub();
	    hub = null;
	    closed = true;
	}
    }

    public synchronized static HubLink getHubLink()
	throws IOException
    {
	if(closed)
	    throw new IOException("HubLinkFactory: wire to hub closed.");
	if(!enabled)
	    throw new IOException("HubLinkFactory: wire to hub disabled.");
	return hub;
    }
}
