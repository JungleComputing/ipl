/* $Id$ */


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WaterWorkerInterface extends Remote {

    public void incAll(int dest, double[][][] f, int size)
            throws RemoteException;

    public double[][][] getPositions() throws RemoteException;

    public double getPotA() throws RemoteException;

    public double getPotR() throws RemoteException;

    public double getPotRF() throws RemoteException;

    public double getTen() throws RemoteException;

    public double getAvgt() throws RemoteException;

    public double getVir() throws RemoteException;
}