package ibis.rmi.server;

import ibis.rmi.RemoteException;

import java.io.Externalizable;

public interface RemoteRef extends Externalizable
{
    public String getRefClass(java.io.ObjectOutput out);
    
    public int remoteHashCode();

    public boolean remoteEquals(RemoteRef obj);

    public String remoteToString();
}
