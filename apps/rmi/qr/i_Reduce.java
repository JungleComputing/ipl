import java.rmi.*;

interface i_Reduce extends Remote { 
	public PivotElt reduce(PivotElt elt) throws RemoteException;
}
