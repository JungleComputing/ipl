import ibis.rmi.*;

interface i_Work extends Remote { 

	public void vote(int var, boolean vote) throws RemoteException;
	public void announce(int var) throws RemoteException;
	public void ready(int cpu) throws RemoteException;
	public boolean workFor(int cpu) throws RemoteException;
	public boolean [] getWork(int cpu) throws RemoteException;
}
