import java.rmi.*;

interface SORInterface extends Remote {

public void putCol(boolean sender, int index, double[] col) throws RemoteException;

}
 
