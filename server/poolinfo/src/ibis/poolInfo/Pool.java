package ibis.poolInfo;

import java.util.ArrayList;

public final class Pool {

    private final String name;

    private final int size;

    private final boolean printEvents;

    private ArrayList<String> addresses;
    
    private ArrayList<String> clusters;

    public Pool(String name, int size, boolean printEvents) {
        this.name = name;
        this.size = size;
        this.printEvents = printEvents;

        addresses = new ArrayList<String>();
        clusters = new ArrayList<String>();

        if (printEvents) {
            System.out.println("PoolInfo: created new pool \"" + name
                    + "\" of size " + size);
        }
    }

    public synchronized int join(String address, String cluster, int size) {
        if (size <= 0) {
            return Service.RESULT_INVALID_SIZE;
        }

        // pool is of different size
        if (size != this.size) {
            return Service.RESULT_UNEQUAL_SIZE;
        }

        // pool is full
        if (addresses.size() >= size) {
            return Service.RESULT_POOL_CLOSED;
        }

        int rank = addresses.size();
        addresses.add(address);
        clusters.add(cluster);

        if (addresses.size() >= size) {
            notifyAll();
        }
        
        if (printEvents) {
            System.out.println("PoolInfo: \"" + address + "@" + cluster
                    + "\" joins pool \"" + name + "\", of size " + size + ", rank = " + rank);
        }

        // wait for everyone else to join too
        while (addresses.size() < size) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        return rank;
    }

    public synchronized String[] getAddresses() {
        return addresses.toArray(new String[0]);
    }

    public synchronized String[] getClusters() {
        return clusters.toArray(new String[0]);
    }

}
