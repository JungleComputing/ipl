/* $Id$ */


import java.rmi.Remote;
import java.rmi.RemoteException;

interface i_Asp extends Remote {

    public void btree_transfer(int[] row, int k, int owner)
            throws RemoteException;

}