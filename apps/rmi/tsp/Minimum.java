import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Minimum extends Remote {
    void set(int minimum) throws RemoteException;
    int get() throws RemoteException;
    void register(MinimumReceiver minReceiver) throws RemoteException;
}
