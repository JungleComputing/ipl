import java.rmi.Remote;
import java.rmi.RemoteException;

public interface i_Server extends Remote {

    public void empty() throws RemoteException;
    public void emptyThreadSwitch() throws RemoteException;
    public void empty(int i0, int i1) throws RemoteException;
    public Object empty(Object b) throws RemoteException;
    public void one_way(Object b) throws RemoteException;

    public void reset() throws RemoteException;
    public void quit() throws RemoteException;

}
