import java.rmi.*;

interface GlobalDataInterface extends Remote {

	public SORInterface [] table(SORInterface me, int node) throws RemoteException;
	public double reduceDiff(double value) throws RemoteException;
}
