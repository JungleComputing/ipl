/* $Id$ */


import java.rmi.Remote;
import java.rmi.RemoteException;

interface Procs extends Remote {

    public Processor[] table(Processor me, int node) throws RemoteException;

}