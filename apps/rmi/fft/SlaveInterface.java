/* $Id$ */


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SlaveInterface extends Remote {

    double[] getValues(int i) throws RemoteException;

    void setValues(int i, double[] values) throws RemoteException;

    double getMatrixChecksum(int i) throws RemoteException;

    void matrixTranspose(int i) throws RemoteException;

    void doTranspose(int i) throws RemoteException;
}