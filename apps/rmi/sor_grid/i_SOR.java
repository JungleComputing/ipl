import java.rmi.Remote;
import java.rmi.RemoteException;

interface i_SOR extends Remote {

public void putCol(boolean sender, int index, double[] col) throws RemoteException;

}
 
