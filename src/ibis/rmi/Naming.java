package ibis.rmi;

import ibis.rmi.registry.LocateRegistry;
import ibis.rmi.registry.Registry;
import java.net.URL;
import java.net.MalformedURLException;

public final class Naming
{
    private static class RegInfo {
	Registry registry;
	String	 host;
	int	 port;
        String	 name;

	public RegInfo(String n) throws MalformedURLException, RemoteException, UnknownHostException {
	    URL	 url;
	    if (n.startsWith("rmi:")) n = n.substring(4);
	    /* Now, if there is a colon before a slash, we have a different protocol (and thus an error). */
	    int colon_index = n.indexOf(':');
	    if (colon_index >= 0) {
		if (colon_index < n.indexOf('/')) {
		    throw new MalformedURLException("Illegal protocol");
		}
	    }

	    url = new URL(new URL("file:"), n);

	    name = url.getFile();

	    if (name.equals("/")) {
		name = null;
	    }
	    else if (name.startsWith("/")) {
		name = name.substring(1);
	    }

	    host = url.getHost();
	    port = url.getPort();

            registry = LocateRegistry.getRegistry(host, port);
	    if(registry == null) {
		throw new UnknownHostException(host);
	    }
	}
    }

    private Naming() {}

    public static Remote lookup(String name) throws NotBoundException, MalformedURLException, UnknownHostException, RemoteException
    {
	RegInfo reg_info = new RegInfo(name);
	return reg_info.registry.lookup(reg_info.name);
    }
    
    public static void bind(String name, Remote obj) throws AlreadyBoundException, MalformedURLException, UnknownHostException, RemoteException
    {
	RegInfo reg_info = new RegInfo(name);
	reg_info.registry.bind(reg_info.name, obj);
    }
    
    public static void unbind(String name) throws RemoteException, NotBoundException, MalformedURLException, UnknownHostException
    {
	RegInfo reg_info = new RegInfo(name);
	reg_info.registry.unbind(reg_info.name);
    }

    public static void rebind(String name, Remote obj) throws RemoteException, MalformedURLException, UnknownHostException
    {
	RegInfo reg_info = new RegInfo(name);
	reg_info.registry.rebind(reg_info.name, obj);
    }
    
    public static String[] list(String name) throws RemoteException, MalformedURLException, UnknownHostException
    {
	RegInfo reg_info = new RegInfo(name);
	String url_prefix = "rmi:";

 	if (reg_info.port > 0 || !reg_info.host.equals("")) url_prefix += "//" + reg_info.host;
	if (reg_info.port > 0) url_prefix += ":" + reg_info.port;
	url_prefix += "/";

	String[] names = reg_info.registry.list();
	for (int i = 0; i < names.length; i++) {
	    names[i] = url_prefix + names[i];
	}
	return names;
    }
}
