
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

class BroadcastObject extends UnicastRemoteObject implements i_BroadcastObject {

    private i_BroadcastObject left, right;

    private int size;

    private int rank;

    double[] update;

    int seqno = -1;

    boolean stop;

    int count = 0;

    int readers = 1;

    BroadcastSender l_sender, r_sender;

    public BroadcastObject(int cpu, int cpus) throws RemoteException {
        super();

        size = cpus;
        rank = cpu;
    }

    public void connect(i_BroadcastObject left, i_BroadcastObject right) {
        this.left = left;
        this.right = right;

        if (left != null) {
            l_sender = new BroadcastSender(this, left);
            readers++;
        }

        if (right != null) {
            r_sender = new BroadcastSender(this, right);
            readers++;
        }

        count = readers;

        if (left != null) {
            l_sender.start();
        }

        if (right != null) {
            r_sender.start();
        }
    }

    public synchronized void put(double[] update, boolean stop)
            throws RemoteException {

        // System.out.println("Put: this = " + this + ", count = " + count + ", readers = " + readers);
        while (count < readers) {
            try {
                wait();
            } catch (Exception e) {
            }
        }
        count = 0;
        this.update = update;
        this.stop = stop;
        seqno++;
        // System.out.println("After Put: seqno = " + seqno + ", count = " + count + ", readers = " + readers);
        notifyAll();
    }

    public synchronized void get(Update d, int seqno) {

        // System.out.println("Get: seqno = " + seqno + ", count = " + count + ", readers = " + readers);
        while (this.seqno < seqno || count == readers) {
            try {
                wait();
            } catch (Exception e) {
            }
        }

        d.update = update;
        d.stop = stop;
        count++;
        // System.out.println("After Get: this = " + this + ", count = " + count + ", readers = " + readers);
        notifyAll();
    }
}