interface RemoteSORVisualizerInterface extends java.rmi.Remote {
	void putData(byte[][] data) throws java.rmi.RemoteException;

	int width() throws java.rmi.RemoteException;
	int height() throws java.rmi.RemoteException;
}
