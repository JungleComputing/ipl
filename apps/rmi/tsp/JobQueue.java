import java.rmi.*;

public interface JobQueue extends Remote {
	Job getJob() throws RemoteException;
	void jobDone() throws RemoteException;
	void allStarted(int total) throws RemoteException;
}
