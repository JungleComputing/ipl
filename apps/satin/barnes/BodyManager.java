import java.rmi.*;

interface BodyManager extends Remote {     
    /**
     * This function is called during force calculation by a node to
     * set the computed accelerations for an array of bodies
     */
    public void setAccs(int[] bNumbers, Vec3[] accs) throws RemoteException;
} 
