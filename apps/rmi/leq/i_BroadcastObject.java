import ibis.rmi.*; 

interface i_BroadcastObject extends Remote { 
	public void put(double [] update, boolean stop) throws RemoteException;
} 
