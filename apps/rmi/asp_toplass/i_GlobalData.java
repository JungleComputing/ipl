import java.rmi.*;

interface i_GlobalData extends Remote {

public i_Asp [] table(i_Asp me, int node) throws RemoteException;

public void start() throws RemoteException;
public void done() throws RemoteException;

}
