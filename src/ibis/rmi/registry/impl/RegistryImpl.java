/* $Id$ */

package ibis.rmi.registry.impl;

import ibis.rmi.AlreadyBoundException;
import ibis.rmi.NotBoundException;
import ibis.rmi.Remote;
import ibis.rmi.RemoteException;
import ibis.rmi.impl.RTS;
import ibis.rmi.registry.Registry;
import ibis.util.IPUtils;

import java.net.InetAddress;

import org.apache.log4j.Logger;

public class RegistryImpl implements Registry {
    static String host = null;

    static int port = 0;

    static Logger logger = ibis.util.GetLogger.getLogger(RegistryImpl.class.getName());

    private String localhostName() {
        String hostname = null;
        InetAddress addr = IPUtils.getLocalHostAddress();
        hostname = addr.getCanonicalHostName();
        if (logger.isDebugEnabled()) {
            logger.debug("localhostName() returns " + hostname);
        }

        return hostname;
    }

    public RegistryImpl(String host, int port) {
        if (host != null && !host.equals("")) {
            try {
                InetAddress adres = InetAddress.getByName(host);
                if (host.equals("localhost")
                        || adres.getHostAddress().equals("127.0.0.1")) {
                    host = localhostName();
                }
                else {
                    host = adres.getCanonicalHostName();
                }
            } catch (java.net.UnknownHostException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Hostname " + host + " is unknown?");
                }
            }
            RegistryImpl.host = host;
        } else {
            RegistryImpl.host = localhostName();
        }
        if (port <= 0) {
            RegistryImpl.port = Registry.REGISTRY_PORT;
        } else {
            RegistryImpl.port = port;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("RegistryImpl<init>: host = " + RegistryImpl.host
                    + " port = " + RegistryImpl.port);
        }

    }

    public RegistryImpl(int port) throws RemoteException {
        this(null, port);
        RTS.createRegistry(port);
    }

    public Remote lookup(String name)
            throws RemoteException, NotBoundException {
        String url = "rmi://" + host + ":" + port + "/" + name;
        try {
            return RTS.lookup(url);
        } catch (NotBoundException e1) {
            throw new NotBoundException(e1.getMessage(), e1);
        } catch (Exception e2) {
            throw new RemoteException(e2.getMessage(), e2);
        }
    }

    public void bind(String name, Remote obj)
            throws RemoteException, AlreadyBoundException {
        String url = "rmi://" + host + ":" + port + "/" + name;
        try {
            RTS.bind(url, obj);
        } catch (AlreadyBoundException e1) {
            throw e1;
        } catch (Exception e2) {
            throw new RemoteException(e2.getMessage(), e2);
        }

    }

    public void rebind(String name, Remote obj) throws RemoteException {
        String url = "rmi://" + host + ":" + port + "/" + name;
        try {
            RTS.rebind(url, obj);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException(e.getMessage(), e);
        }
    }

    public void unbind(String name) throws RemoteException, NotBoundException {
        String url = "rmi://" + host + ":" + port + "/" + name;
        try {
            RTS.unbind(url);
        } catch (NotBoundException e1) {
            throw e1;
        } catch (Exception e2) {
            throw new RemoteException(e2.getMessage(), e2);
        }
    }

    public String[] list() throws RemoteException {
        try {
            String url = "rmi://" + host + ":" + port + "/";
            String[] names = RTS.list(url);
            if (logger.isDebugEnabled()) {
                logger.debug(names.length + " names bound in the registry");
            }
            return names;
        } catch (Exception e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }
}
