import java.rmi.Remote;
import java.rmi.RemoteException;

interface i_BroadcastObject extends Remote { 
    public void send(int broadcast, Object o, int owner) throws RemoteException;
}
