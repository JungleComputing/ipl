import java.rmi.*;

interface GlobalDataInterface extends Remote {

	public SORInterface [] table(SORInterface me, int node) throws RemoteException;
	public double reduceDiff(double value) throws RemoteException;


	// Used for visualization, downsample/enlarge to the given size.
	public void setRawDataSize(int width, int height) throws RemoteException;

	// Used for visualization, downsample/enlarge to the given size.
	public float[][] getRawData() throws RemoteException;
}
