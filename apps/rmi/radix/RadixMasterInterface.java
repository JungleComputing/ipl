
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RadixMasterInterface extends Remote {

    public int logon(String workerName) throws RemoteException;

    public Job get_Job(int id) throws RemoteException;

    public void sync() throws RemoteException;

    public void sync2() throws RemoteException;

    public void putStats(int host, Stats stat) throws RemoteException;

    public TreeInterface[] getTrees(TreeInterface tree, int cpunum)
            throws RemoteException;

    public PartsInterface[] getParts(PartsInterface part, int cpunum)
            throws RemoteException;
}