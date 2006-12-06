/* $Id$ */

package ibis.rmi.registry.impl;

import ibis.rmi.AlreadyBoundException;
import ibis.rmi.NotBoundException;
import ibis.rmi.RemoteException;
import ibis.rmi.Remote;
import ibis.rmi.impl.RTS;
import ibis.rmi.registry.Registry;

import java.util.HashMap;

import java.net.InetAddress;

import org.apache.log4j.Logger;

public class RegistryImpl extends ibis.rmi.server.UnicastRemoteObject
        implements Registry {

    static Logger logger
            = ibis.util.GetLogger.getLogger(RegistryImpl.class.getName());

    static String host = null;

    private int port = 0;

    HashMap remotes = new HashMap();

    public RegistryImpl(int port) throws RemoteException {
        if (port <= 0) {
            this.port = Registry.REGISTRY_PORT;
        } else {
            this.port = port;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("RegistryImpl<init>: " + " port = " + this.port);
        }
        RTS.createRegistry(port, this);
    }

    public synchronized Remote lookup(String name) throws NotBoundException {
        if (! remotes.containsKey(name)) {
            throw new NotBoundException("lookup: " + name + " not bound");
        }
        return (Remote) remotes.get(name);
    }

    public synchronized void bind(String name, Remote obj)
            throws AlreadyBoundException {
        if (remotes.containsKey(name)) {
            throw new AlreadyBoundException("bind: " + name + " already bound");
        }
        remotes.put(name, obj);
    }

    public synchronized void rebind(String name, Remote obj) {
        remotes.put(name, obj);
    }

    public synchronized void unbind(String name) throws NotBoundException {
        if (! remotes.containsKey(name)) {
            throw new NotBoundException("unbind: " + name + " not bound");
        }
        remotes.remove(name);
    }

    public synchronized String[] list() {
        return (String[]) remotes.keySet().toArray(new String[0]);
    }
}
