package ibis.rmi.registry;

import ibis.rmi.impl.RTS;
import ibis.rmi.RemoteException;

/**
 * The <code>LocateRegistry</code> class is a container class for methods that
 * are used to obtain a reference to a remote registry or to create a registry.
 */
public final class LocateRegistry
{
    private static String registryPkgPrefix =
        System.getProperty("java.rmi.registry.packagePrefix", "ibis.rmi.registry.impl");
			  
    private static RegistryHandler handler = null;
    
    private LocateRegistry() {}

    /**
     * Returns a reference to the remote <code>Registry</code> for the local
     * host on the default port.
     * @return a stub for the remote registry.
     * @exception RemoteException if the stub could not be created.
     */
    public static Registry getRegistry() throws RemoteException
    {
	return getRegistry(null, Registry.REGISTRY_PORT);
    }

    /**
     * Returns a reference to the remote <code>Registry</code> for the local
     * host on the specified port.
     * @param port the port on which the registry accepts requests
     * @return a stub for the remote registry.
     * @exception RemoteException if the stub could not be created.
     */
    public static Registry getRegistry(int port) throws RemoteException
    {
	return getRegistry(null, port);
    }
    
    /**
     * Returns a reference to the remote <code>Registry</code> for the
     * specified host on the default port.
     * @param host host for the remote registry
     * @return a stub for the remote registry.
     * @exception RemoteException if the stub could not be created.
     */
    public static Registry getRegistry(String host) throws RemoteException
    {
	return getRegistry(host, Registry.REGISTRY_PORT);
    }
    
    /**
     * Returns a reference to the remote <code>Registry</code> for the
     * specified host on the specified port.
     * @param host host for the remote registry
     * @param port the port on which the registry accepts requests
     * @return a stub for the remote registry.
     * @exception RemoteException if the stub could not be created.
     */
    public static Registry getRegistry(String host, int port) throws RemoteException
    {
	if (handler != null) {
	    return handler.registryStub(host, port);
	}

	throw new RemoteException("Registry handler not present");
    }

    /**
     * Creates and exports a <code>Registry</code> on the local host.
     * This registry will accept requests on the specified port.
     * @param port the port on which the registry accepts requests
     * @return the registry.
     */
    public static Registry createRegistry(int port) throws RemoteException
    {
	if (handler != null) {
	    return handler.registryImpl(port);
	}

	throw new RemoteException("Registry handler not present");
    }

    static {
        String classname = registryPkgPrefix + ".RegistryHandler";
	try {
	    String hostname = RTS.getHostname();
	    // Just to make sure that RTS is initialized and that there is an Ibis.
	    Class cl = Class.forName(classname);
	    handler = (RegistryHandler)(cl.newInstance());
	} catch (Exception e) {
	}
    }
}
