package ibis.rmi.registry.impl;

import ibis.rmi.*;
import ibis.rmi.registry.*;

import java.net.InetAddress;

public class RegistryImpl implements Registry
{
    protected static String host = null;
    protected static int port = 0;
    
    private String localhostName() {
	String hostname = null;
	try {
	    hostname = InetAddress.getLocalHost().getHostName();
	    
	    /* This stuff needed to get a proper hostname.
	       If not included, you might get localhost.localdomain.
	    */
	    InetAddress adres = InetAddress.getByName(hostname);
	    adres = InetAddress.getByName(adres.getHostAddress());
	    hostname = adres.getHostName();
	} catch (java.net.UnknownHostException e) {
if(RTS.DEBUG)	
	    System.err.println("hmmm... local host is unknown?");
	}
if(RTS.DEBUG) 
	System.out.println("localhostName() returns " + hostname);

	return hostname;
    } 
    
    public RegistryImpl(String host, int port) 
    {
	if (host != null && ! host.equals("")) {
	    this.host = host;
	} else {
	    this.host = localhostName();
	}
	if (port <= 0) {
	    this.port = Registry.REGISTRY_PORT;
	} else {	    
	    this.port = port;
	}
if(RTS.DEBUG)  {
    System.out.println("RegistryImpl<init>: host = "  + this.host + " port = " + this.port);
}
	
    }
    
    public RegistryImpl(int port) throws RemoteException
    {
	this(null, port);    
	RTS.createRegistry(port);	
    }
    
    public Remote lookup(String name)
	throws RemoteException, NotBoundException
    {
	String url = "rmi://" + host + ":" + port + "/" + name;
	try {
	    return RTS.lookup(url);
	} catch (NotBoundException e1) {
	    throw new NotBoundException(e1.getMessage());
	} catch (Exception e2) {
	    throw new RemoteException(e2.getMessage());
	}
    }
    
    public void bind(String name, Remote obj)
	throws RemoteException, AlreadyBoundException, AccessException
    {
	checkreg();
	String url = "rmi://" + host + ":" + port + "/" + name;
	try {
	    RTS.bind(url, obj);
	} catch (AlreadyBoundException e1) {
	    throw new AlreadyBoundException(e1.getMessage());
	} catch (Exception e2) {
if (RTS.DEBUG)
	    e2.printStackTrace();		    
	    throw new RemoteException(e2.getMessage());
	}

    }
    
    public void rebind(String name, Remote obj)
	throws RemoteException, AccessException
    {
	checkreg();
	String url = "rmi://" + host + ":" + port + "/" + name;	
	try {
	    RTS.rebind(url, obj);
	} catch (Exception e) {
	    throw new RemoteException(e.getMessage());
	}
    }
    
    public void unbind(String name)
	throws RemoteException, NotBoundException, AccessException
    {
	checkreg();
	String url = "rmi://" + host + ":" + port + "/" + name;	
	try {
	    RTS.unbind(url);
	} catch (NotBoundException e1) {
	    throw new NotBoundException(e1.getMessage());
	} catch (Exception e2) {
	    throw new RemoteException(e2.getMessage());
	}
    }
    
    public String[] list()
	throws RemoteException, AccessException
    {
	try {
	    String url = "rmi://" + host + ":" + port + "/";
	    String[] names = RTS.list(url);
if (RTS.DEBUG) 
	    System.out.println(names.length + " names bound in the registry");
	    return names;	    
	} catch (Exception e) {
	    throw new RemoteException(e.getMessage());
	}
    }
    
    private void checkreg() 
	throws AccessException
    {
	String localhost = localhostName();
	if (!host.equals(localhost)) {
	//    throw new AccessException("Registry: " + host + "!=" + localhost);
	}
    }
    
}
