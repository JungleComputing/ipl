import java.rmi.Remote;
import java.rmi.RemoteException;

interface Processor extends Remote {

  void barrier() throws RemoteException;

  void setBodyCount( int Count ) throws RemoteException;

  MinMax setMinMax( MinMax minmax ) throws RemoteException;

  void setTotalExchangeInt( int Source, int Value ) throws RemoteException;

  void setExchangeBodies( int source, Body Bodies[] ) throws RemoteException;
  void setExchangeIntArray( int source, int array[], int offset, int size ) throws RemoteException;

  public void setEssential( int Source, int bCount, SendBody [] b, int cCount, CenterOfMass [] c ) throws RemoteException;

  public void setEssential( int Source, int bCount, double [] bp, int cCount, double [] cp ) throws RemoteException;

}
