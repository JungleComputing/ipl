package ibis.rmi.registry;

import ibis.rmi.AlreadyBoundException;
import ibis.rmi.NotBoundException;
import ibis.rmi.Remote;
import ibis.rmi.RemoteException;

public interface Registry extends Remote
{
    public static final int REGISTRY_PORT = 1099;
    
    public Remote lookup(String name) throws RemoteException, NotBoundException;

    public void bind(String name, Remote obj) throws RemoteException, AlreadyBoundException;
    
    public void unbind(String name) throws RemoteException, NotBoundException;

    public void rebind(String name, Remote obj) throws RemoteException;

    public String[] list() throws RemoteException;
}
