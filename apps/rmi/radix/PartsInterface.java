
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PartsInterface extends Remote {

    public void put(int[] prockeys) throws RemoteException;

    public int[] getPart() throws RemoteException;
}