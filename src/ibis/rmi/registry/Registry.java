package ibis.rmi.registry;

import ibis.rmi.AlreadyBoundException;
import ibis.rmi.NotBoundException;
import ibis.rmi.Remote;
import ibis.rmi.RemoteException;

/**
 * A <code>Registry</code> is a remote interface to a remote-object registry.
 * It provides methods for binding and looking-up remote object references
 * bound to names.
 */
public interface Registry extends Remote {
    /** Well known port for RMI registry. */
    public static final int REGISTRY_PORT = 1099;

    /**
     * Returns the remote reference bound to the specified name.
     *
     * @param name the name to look up
     * @return a reference to the remote object
     * @exception NotBoundException if the name is not bound
     * @exception RemoteException if communication with the registry failed.
     */
    public Remote lookup(String name) throws RemoteException, NotBoundException;

    /**
     * Binds the specified remote reference to the specified name.
     *
     * @param name the name to bind the reference to
     * @param obj the remote reference
     * @exception AlreadyBoundException is the name is already bound
     * @exception RemoteException if communication with the registry failed.
     */
    public void bind(String name, Remote obj) throws RemoteException,
            AlreadyBoundException;

    /**
     * Removes the binding for the specified name.
     *
     * @param name the name to unbind
     * @exception NotBoundException if the name is not bound
     * @exception RemoteException if communication with the registry failed.
     */
    public void unbind(String name) throws RemoteException, NotBoundException;

    /**
     * Rebinds the specified remote reference to the specified name. Any
     * existing binding for the name is replaced.
     *
     * @param name the name to bind the reference to
     * @param obj the remote reference
     * @exception RemoteException if communication with the registry failed.
     */
    public void rebind(String name, Remote obj) throws RemoteException;

    /**
     * Returns an array of names bound in this registry.
     * @return an array of names
     * @exception RemoteException if communication with the registry failed.
     */
    public String[] list() throws RemoteException;
}