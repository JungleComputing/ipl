/* $Id:$ */

package ibis.rmi.registry.impl;

import ibis.rmi.AlreadyBoundException;
import ibis.rmi.NotBoundException;
import ibis.rmi.Remote;
import ibis.rmi.RemoteException;
import ibis.rmi.impl.RTS;
import ibis.rmi.registry.Registry;

import java.io.IOException;
import java.util.HashMap;

import java.net.InetAddress;

import org.apache.log4j.Logger;

public final class RegistryWrapper implements Registry {

    static Logger logger
            = Logger.getLogger(RegistryWrapper.class.getName());

    private Registry registry = null;
    private int port = 0;
    private String host = null;
    private boolean initialized = false;

    public RegistryWrapper(Registry registry) {
        this.registry = registry;
        initialized = true;
    }

    public RegistryWrapper(String host, int port) {
        this.host = host;
        if (port <= 0) {
            this.port = Registry.REGISTRY_PORT;
        } else {
            this.port = port;
        }
    }

    private void init() throws RemoteException {
        if (! initialized) {
            initialized = true;
            try {
                registry = RTS.lookupRegistry(host, port);
            } catch(IOException e) {
                throw new RemoteException("Could not find registry", e);
            }
        }
    }

    public synchronized Remote lookup(String name) throws 
            RemoteException, NotBoundException {
        if (logger.isDebugEnabled()) {
            logger.debug("lookup " + name);
        }
        init();
        return registry.lookup(name);
    }

    public synchronized void bind(String name, Remote obj)
            throws RemoteException, AlreadyBoundException {
        if (logger.isDebugEnabled()) {
            logger.debug("bind " + name);
        }
        init();
        registry.bind(name, obj);
    }

    public synchronized void rebind(String name, Remote obj)
            throws RemoteException {
        if (logger.isDebugEnabled()) {
            logger.debug("rebind " + name);
        }
        init();
        registry.rebind(name, obj);
    }

    public synchronized void unbind(String name)
            throws RemoteException, NotBoundException {
        if (logger.isDebugEnabled()) {
            logger.debug("unbind " + name);
        }
        init();
        registry.unbind(name);
    }

    public synchronized String[] list() throws RemoteException {
        init();
        return registry.list();
    }
}
