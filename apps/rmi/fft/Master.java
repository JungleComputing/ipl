
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

class Master extends UnicastRemoteObject implements MasterInterface {

    Barrier innerBarrier;

    int cpus, M, rowsperproc;

    private int num_nodes;

    private SlaveInterface[] slaves;

    Master(Registry reg, int cpus, int M, int rowsperproc) throws Exception {

        super();

        this.cpus = cpus;
        this.M = M;
        this.rowsperproc = rowsperproc;
        innerBarrier = new Barrier(cpus);
        slaves = new SlaveInterface[cpus];

        System.out.println("binding FFT_Master ...");
        reg.bind("FFT_Master", this);
        System.out.println("binding FFT_Master done");
    }

    public synchronized SlaveInterface[] table(SlaveInterface me, int node)
            throws RemoteException {
        num_nodes++;
        slaves[node] = me;
        if (num_nodes == cpus)
            notifyAll();
        else
            while (num_nodes < cpus) {
                try {
                    wait();
                } catch (Exception e) {
                    throw new RemoteException(e.toString());
                }
            }
        return slaves;
    }

    void unbind(Registry reg) {
        try {
            reg.unbind("FFT_Master");
        } catch (Exception e) {
            System.out.println("Couldn't unbind master " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void sync() throws java.rmi.RemoteException {
        innerBarrier.sync();
    }
}