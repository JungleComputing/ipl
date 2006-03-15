/* $Id$ */

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

class ProcsImpl extends UnicastRemoteObject implements Procs {

    private int total_num;

    // Used for the initial exchange of stubs.
    private Processor[] nodes;

    private int num_nodes;

    ProcsImpl(int nhosts) throws RemoteException {
        total_num = nhosts;
        nodes = new Processor[total_num];
        num_nodes = 0;
    }

    public synchronized Processor[] table(Processor me, int node)
        throws RemoteException {

        // Note: This function can only be used once !
        num_nodes++;

        nodes[node] = me;

        if (num_nodes == total_num) {
            notifyAll();
        } else while (num_nodes < total_num) {
            try {
                wait();
            } catch (Exception e) {
                throw new RemoteException(e.toString());
            }
        }

        return nodes;
    }

}
