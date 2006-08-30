/* $Id$ */


import java.rmi.Remote;
import java.rmi.RemoteException;

interface i_Data extends Remote {

    public void put(int cpu, int removed, long checks, long time, int modif,
            int change_ops, int revise) throws RemoteException;

    public String result() throws RemoteException;

}