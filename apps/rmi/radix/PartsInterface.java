import ibis.rmi.*;

public interface PartsInterface extends Remote{

    public void put(int[] prockeys) throws RemoteException;
    public int[] getPart()throws RemoteException;
}
