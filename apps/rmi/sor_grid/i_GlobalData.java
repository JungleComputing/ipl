import java.rmi.Remote;
import java.rmi.RemoteException;

interface i_GlobalData extends Remote {

	public i_SOR [] table(i_SOR me, int node) throws RemoteException;
	public double reduceDiff(double value) throws RemoteException;

	public double[] scatter2all(int rank, double value) throws RemoteException;
	public void sync() throws RemoteException;
}
