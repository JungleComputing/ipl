package ibis.rmi.server;

import java.io.Externalizable;
import ibis.rmi.RemoteException;

public interface RemoteRef extends Externalizable
{
    public final static String packagePrefix =
        System.getProperty("java.rmi.server.packagePrefix", "ibis.rmi.server");
    
    public RemoteCall newCall(RemoteObject obj, Operation[] op, int opnum, long hash) throws RemoteException;
    
    public void invoke(RemoteCall call) throws Exception;
    
    public void done(RemoteCall call) throws RemoteException;
    
    public String getRefClass(java.io.ObjectOutput out);
    
    public int remoteHashCode();

    public boolean remoteEquals(RemoteRef obj);

    public String remoteToString();
}
