import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

class Parts extends UnicastRemoteObject implements PartsInterface{

    int[] part;
	
    Parts(int length)throws RemoteException{
	super();
	part = new int[length];
    }
	
    Parts(int[] part2)throws RemoteException{
	super();
	part = new int[part2.length];
	for(int i = 0; i < part2.length; i++){
	    part[i] = part2[i];
	}
		
    }
	
    public synchronized void  put(int[] procKeys) throws RemoteException{
	int offset, cnt2;
	int l = 0;

	while((cnt2 = procKeys[l++]) > 0){
	    offset = procKeys[l++];
	    for(int j = 0; j < cnt2; j++){
		part[offset + j] = procKeys[l + j];
	    }
	    l+=cnt2;
	}
    }

    public int[] getPart()throws RemoteException{
	return part;
    }
}
