/* $Id$ */


import java.rmi.Remote;
import java.rmi.RemoteException;

interface i_Data extends Remote {
    public Remote[] signup(int cpu, String name) throws RemoteException;

    public void put(int cpu, long time) throws RemoteException;
}