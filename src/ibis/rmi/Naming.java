/* $Id$ */

package ibis.rmi;

import ibis.rmi.registry.LocateRegistry;
import ibis.rmi.registry.Registry;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * The <code>Naming</code> class provides static methods for storing and
 * obtaining references to remote objects. These methods take a name in URL
 * format as one of its arguments.
 */
public final class Naming {
    private static class RegInfo {
        Registry registry;

        String host;

        int port;

        String name;

        public RegInfo(String n) throws MalformedURLException, RemoteException,
                UnknownHostException {
            URL url;
            if (n.startsWith("rmi:")) {
                n = n.substring(4);
            }
            /* Now, if there is a colon before a slash, we have a different
             * protocol (and thus an error).
             */
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
            } else if (name.startsWith("/")) {
                name = name.substring(1);
            }

            host = url.getHost();
            port = url.getPort();

            registry = LocateRegistry.getRegistry(host, port);
            if (registry == null) {
                throw new UnknownHostException(host);
            }
        }
    }

    /**
     * Private constructor to prevent instantiating of this class.
     */
    private Naming() {
        // prevent instantiating
    }

    /**
     * Returns a reference for the remote object associated with the
     * specified name.
     *
     * @param name the name in URL format
     * @return a stub for the remote object
     * @exception NotBoundException if name is not bound
     * @exception MalformedURLException if the name is not appropriately
     *  formatted
     * @exception RemoteException if the registry could not be found
     */
    public static Remote lookup(String name) throws NotBoundException,
            MalformedURLException, RemoteException {
        RegInfo reg_info = new RegInfo(name);
        return reg_info.registry.lookup(reg_info.name);
    }

    /**
     * Binds the specified name to the specified remote object.
     *
     * @param name the name in URL format
     * @param obj a reference to the remote object
     * @exception RemoteException if the registry could not be found
     * @exception MalformedURLException if the name is not appropriately
     *  formatted
     * @exception AlreadyBoundException if the name is already bound
     */
    public static void bind(String name, Remote obj)
            throws AlreadyBoundException, MalformedURLException,
            RemoteException {
        RegInfo reg_info = new RegInfo(name);
        reg_info.registry.bind(reg_info.name, obj);
    }

    /**
     * Removes the binding for the specified name.
     *
     * @param name the name in URL format
     * @exception NotBoundException if name is not bound
     * @exception MalformedURLException if the name is not appropriately
     *  formatted
     * @exception RemoteException if the registry could not be found
     */
    public static void unbind(String name) throws RemoteException,
            NotBoundException, MalformedURLException {
        RegInfo reg_info = new RegInfo(name);
        reg_info.registry.unbind(reg_info.name);
    }

    /**
     * Rebinds the specified name to the specified remote object. A
     * existing binding is for the name is replaced.
     *
     * @param name the name in URL format
     * @param obj a reference to the remote object
     * @exception RemoteException if the registry could not be found
     * @exception MalformedURLException if the name is not appropriately
     *  formatted
     */
    public static void rebind(String name, Remote obj) throws RemoteException,
            MalformedURLException {
        RegInfo reg_info = new RegInfo(name);
        reg_info.registry.rebind(reg_info.name, obj);
    }

    /**
     * Returns an array of all names bound in the registry.
     *
     * @param name a registry name in URL format.
     * @return an array of names.
     * @exception MalformedURLException if the name is not appropriately
     *  formatted
     * @exception RemoteException if the registry could not be found
     */
    public static String[] list(String name) throws RemoteException,
            MalformedURLException {
        RegInfo reg_info = new RegInfo(name);
        String url_prefix = "rmi:";

        if (reg_info.port > 0 || !reg_info.host.equals("")) {
            url_prefix += "//" + reg_info.host;
        }
        if (reg_info.port > 0) {
            url_prefix += ":" + reg_info.port;
        }
        url_prefix += "/";

        String[] names = reg_info.registry.list();
        for (int i = 0; i < names.length; i++) {
            names[i] = url_prefix + names[i];
        }
        return names;
    }
}
