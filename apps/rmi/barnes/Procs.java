import ibis.rmi.*;

interface Procs extends Remote {

public Processor [] table(Processor me, int node) throws RemoteException;

}
