import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MinimumReceiver extends Remote {
    public void update(int min) throws RemoteException;
}
