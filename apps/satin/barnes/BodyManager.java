import java.rmi.*;

interface BodyManager extends Remote {     
    /**
     * This function is called during force calculation by a node to
     * set the computed acceleration for a body
     */
    public void setAcc(int index, Vec3 acc) throws RemoteException;
} 
