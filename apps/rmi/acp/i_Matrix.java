/* $Id$ */


import java.rmi.Remote;
import java.rmi.RemoteException;

interface i_Matrix extends Remote {

    public boolean[][] getValue() throws RemoteException;

    public void change(int x, int[] list_change, int poz_change)
            throws RemoteException;
}