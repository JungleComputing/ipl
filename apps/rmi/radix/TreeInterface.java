
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TreeInterface extends Remote {

    public void waitPauze(int node) throws RemoteException;

    public void clearPauze(int node) throws RemoteException;

    public void setPauze(int node) throws RemoteException;

    public void putDensity(int node, int[] density) throws RemoteException;

    public void putRank(int node, int[] rank) throws RemoteException;

    public void putSet(int node, int[] density, int[] rank)
            throws RemoteException;

    public int[] getDensity(int node) throws RemoteException;

    public int[] getRank(int node) throws RemoteException;

}