/* $Id$ */


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MasterInterface extends Remote {

    public SlaveInterface[] table(SlaveInterface me, int node)
            throws RemoteException;

    void sync() throws RemoteException;
}