package ibis.connect.socketFactory;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Enumeration;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.lang.reflect.Constructor;

import ibis.connect.util.MyDebug;

/* A generalized SocketFactory which supports:
   -- client/server connection scheme
   -- brokered connections through a control link
 */
public class ExtSocketFactory
{
    /** Vector of SocketTypes in the order of preference
     * for the current strategy.
     */
    private static Vector types = new Vector();

    /** Map which converts a SocketType name into
     * the class name which implements it.
     */
    private static Hashtable typesTable = new Hashtable();

    private static SocketType defaultClientServer = null;
    private static SocketType defaultBrokeredLink = null;

    // Some possible default strategies...
    // -1- Plain TCP only- no firewall support.
    private static final String[] strategyTCP = 
    { "PlainTCP" };

    // -2- full range of conection methods: supports firewalls
    private static final String[] strategyFirewall = 
    { "TCPSplice", "RoutedMessages", "PlainTCP" }; 

    // -3- supports firewall for control only, no splicing. Usefull for tests only.
    private static final String[] strategyControl = 
    { "RoutedMessages", "PlainTCP" };
     
    // -4- TCP splicing only- for tests.
    private static final String[] strategySplicing = 
    { "TCPSplice", "PlainTCP" }; 

    // Pick one of the above choices for defaults
    private static String[] defaultTypes = strategyTCP;

    /* static constructor
     */
    static {
	if(MyDebug.VERBOSE())
	    System.out.println("# ### ExtSocketFactory: starting configuration.");
	
	// init types table
	typesTable.put("PlainTCP", 
		       "ibis.connect.socketFactory.PlainTCPSocketType");
	typesTable.put("TCPSplice",
		       "ibis.connect.tcpSplicing.TCPSpliceSocketType");
	typesTable.put("RoutedMessages", 
		       "ibis.connect.routedMessages.RoutedMessagesSocketType");

	Properties p = System.getProperties();
	String bl = p.getProperty("ibis.connect.data_links");
	String cs = p.getProperty("ibis.connect.control_links");
	if(bl != null && cs != null) {
	    defaultClientServer = loadSocketType(cs);
	    defaultBrokeredLink = loadSocketType(bl);
	} else {
	    if(MyDebug.VERBOSE())
		System.out.println("# Loading defaults...");
	    for(int i=0; i<defaultTypes.length; i++)
		{
		    String n = defaultTypes[i];
		    loadSocketType(n);
		}
	    defaultClientServer = findClientServerType();
	    defaultBrokeredLink = findBrokeredType();
	}
	if(MyDebug.VERBOSE()) {
	    System.out.println("# Default for client-server: " +
			       defaultClientServer.getSocketTypeName());
	    System.out.println("# Default for brokered link: " +
			       defaultBrokeredLink.getSocketTypeName());
	    System.out.println("# ### ExtSocketFactory: configuration ok.");
	}
    }

    /* static destructor
     */
    public static void shutdown()
    {
	for(int i=0; i<types.size(); i++)
	    {
		SocketType t = (SocketType)types.get(i);
		t.destroySocketType();
	    }
    }

    /* loads a SocketType into the factory
     *   name: a fully-qualified class name which extends SocketType
     */
    private static synchronized SocketType loadSocketType(String socketType)
    {
	SocketType t = null;
	Constructor cons;
	String className = (String)typesTable.get(socketType);
	if(className == null) {
	    System.out.println("# ExtSocketFactory: socket type "+socketType+" not found.");
	    System.out.println("#   known types are:");
	    Enumeration e = typesTable.keys();
	    while(e.hasMoreElements()) {
		System.out.println((String)e.nextElement());
	    }
	    throw new Error("ExtSocketFactory: socket type "+socketType+" not found.");
	}
	try {
	    Class c = Class.forName(className);
	    cons = c.getConstructor(null);
	} catch(Exception e) {
	    System.out.println("# ExtSocketFactory: error while loading socket type.");
	    throw new Error("ExtSocketFactory: class not found: "+className, e);
	}
	try {
	    t = (SocketType)cons.newInstance(null);
	    if (MyDebug.VERBOSE()) {
		System.out.println("# Registering socket type: "+t.getSocketTypeName());
		System.out.println("#   class name: "+t.getClass().getName());
		System.out.println("#   supports client/server:  "+t.supportsClientServer());
		System.out.println("#   supports brokered links: "+t.supportsBrokeredLinks());
	    }
	    types.add(t);
	} catch(Exception e) {
	    System.out.println("# ExtSocketFactory: *not* adding: "+t.getSocketTypeName());
	}
	return t;
    }

    // Bootstrap client sockets: Socket(addr, port);
    public static Socket createClientSocket(InetAddress addr, int port)
	throws IOException
    {
	Socket s = null;
	SocketType t = defaultClientServer;
	if(t == null)
	    throw new Error("no socket type found!");
	ClientServerSocketFactory f = null;
	try {
	    f = (ClientServerSocketFactory)t;
	} catch(Exception e) {
	    System.out.println("SocketFactory: SocketType "+
			       t.getSocketTypeName()+
			       " does not support client/sever connection establishment.");
	    throw new Error(e);
	}
	s = f.createClientSocket(addr, port);
	return s;
    }

    // Bootstrap server sockets: ServerSocket(port, backlog, addr);
    public static ServerSocket createServerSocket(int port, int backlog, InetAddress addr)
	throws IOException
    {
	return createServerSocket(new InetSocketAddress(addr, port), backlog);
    }
    public static ServerSocket createServerSocket(InetSocketAddress addr, int backlog)
	throws IOException
    {
	ServerSocket s = null;
	SocketType t = defaultClientServer;
	if(t == null)
	    throw new Error("no socket type found!");
	ClientServerSocketFactory f = null;
	try {
	    f = (ClientServerSocketFactory)t;
	} catch(Exception e) {
	    System.out.println("SocketFactory: SocketType "+
			       t.getSocketTypeName()+
			       " does not support client/sever connection establishment.");
	    throw new Error(e);
	}
	s = f.createServerSocket(addr, backlog);
	return s;
    }

    // Data connections: when a service link is available
    public static Socket createBrokeredSocket(InputStream in, OutputStream out, boolean hintIsServer)
	throws IOException
    {
	Socket s = null;
	SocketType t = defaultBrokeredLink;
	if(t == null)
	    throw new Error("no socket type found!");
	BrokeredSocketFactory f = null;
	try {
	    f = (BrokeredSocketFactory)t;
	} catch(Exception e) {
	    System.out.println("SocketFactory: SocketType "+
			       t.getSocketTypeName()+
			       " does not support brokered connection establishment.");
	    throw new Error(e);
	}
	s = f.createBrokeredSocket(in, out, hintIsServer);
	return s;
    }

    public static Socket createBrokeredSocketFromClientServer(ClientServerSocketFactory type,
							      InputStream in, OutputStream out,
							      boolean hintIsServer)
	throws IOException
    {
	Socket s = null;
	/* TODO: the 'hint' shouldn't be needed. There should be an
	 * election to chose who is client and who is server.
	 */
	if(hintIsServer) {
	    ServerSocket server = type.createServerSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0), 1);
	    ObjectOutputStream os = new ObjectOutputStream(out);
	    Hashtable lInfo = new Hashtable();
	    lInfo.put("tcp_address", server.getInetAddress());
	    lInfo.put("tcp_port",    new Integer(server.getLocalPort()));
	    os.writeObject(lInfo);
	    os.flush();
	    os.close();
	    s = server.accept();
	} else {
	    ObjectInputStream is = new ObjectInputStream(in);
	    Hashtable rInfo = null;
	    try {
		rInfo = (Hashtable)is.readObject();
	    } catch (ClassNotFoundException e) {
		throw new Error(e);
	    }
	    InetAddress raddr =  (InetAddress)rInfo.get("tcp_address");
	    int         rport = ((Integer)    rInfo.get("tcp_port")   ).intValue();
	    is.close();
	    s = type.createClientSocket(raddr, rport);
	}
	return s;
    }

    private static synchronized SocketType findClientServerType()
    {
	for(int i=0; i<types.size(); i++)
	    {
		SocketType t = (SocketType)types.get(i);
		if(t.supportsClientServer())
		    {
			if (MyDebug.VERBOSE()) {
			    System.out.println("# Selected type: '"+
					       t.getSocketTypeName()+
					       "' for client/server connection.");
			}
			return t;
		    }
	    }
	System.out.println("# ExtSocketFactory: warning- no SocketType found for client/server link!");
	return null;
    }
    private static synchronized SocketType findBrokeredType()
    {
	for(int i=0; i<types.size(); i++)
	    {
		SocketType t = (SocketType)types.get(i);
		if(t.supportsBrokeredLinks())
		    {
			if (MyDebug.VERBOSE()) {
			    System.out.println("# Selected type: '"+
					       t.getSocketTypeName()+
					       "' for brokered link.");
			}
			return t;
		    }
	    }
	System.out.println("# ExtSocketFactory: warning- no SocketType found for brokered links!");
	return null;
    }
}

