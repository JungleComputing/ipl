import java.rmi.*;

public interface MasterInterface extends Remote {

    public SlaveInterface[] table(SlaveInterface me, int node) 
                throws RemoteException;

    void sync() throws RemoteException;
}
