import java.rmi.Remote;
import java.rmi.RemoteException;

interface i_Reduce extends Remote { 
	public PivotElt reduce(PivotElt elt) throws RemoteException;
}
