import java.rmi.*;

public interface WaterMasterInterface extends Remote {

    public int logon(String workerName, WaterWorkerInterface w) throws RemoteException;
    public WaterWorkerInterface[] getWorkers() throws RemoteException;
    public Job getJob(int id) throws RemoteException;
    public void sync() throws RemoteException;
    public void sync1() throws RemoteException;
}
