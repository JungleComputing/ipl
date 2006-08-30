/* $Id$ */

/*
 * Asp.java:
 * 	All-pairs shortest path implementation based on Floyd's
 * 	algorithms. The distance matrix's rows are block-striped
 * 	across all processors. This allows for pipelining, but leads
 * 	to flow-control problems.
 *
 */

import ibis.util.PoolInfo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

class Asp extends UnicastRemoteObject implements i_Asp {

    int[][] tab;

    int nodes, nrClusters, rank, cluster, n;

    int lb, ub;

    i_Asp[] receivers;

    boolean use_threads, use_thread_pool;

    boolean print_result;

    BtreePoolSender leftSender, rightSender;

    PoolInfo info;

    i_GlobalData global;

    Asp(i_GlobalData g, int n, boolean use_threads, boolean use_thread_pool,
            boolean print_result) throws RemoteException {

        this.global = g;

        this.n = n;

        try {
            this.info = PoolInfo.createPoolInfo();
        } catch (Exception e) {
            throw new Error("Problem in PoolInfo", e);
        }

        this.nodes = info.size();
        this.rank = info.rank();

        this.use_threads = use_threads;
        this.use_thread_pool = use_thread_pool;
        this.print_result = print_result;

        leftSender = new BtreePoolSender();
        rightSender = new BtreePoolSender();

        leftSender.start();
        rightSender.start();

        get_bounds();
        init_tab();
    }

    void setTable(i_Asp[] receivers) {
        this.receivers = receivers;
    }

    synchronized void transfer(int[] row, int k) {
        tab[k] = row;
        notifyAll();
    }

    void bcast(int k, int owner) {
        if (rank == owner) {
            btree_bcast(k, owner);
        } else {
            while (tab[k] == null) {
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //----------------------------------------------------------------------------

    int rel_rank(int abs, int root, int size) {
        return ((size + abs - root) % size);
    }

    int abs_rank(int rel, int root, int size) {
        return ((rel + root) % size);
    }

    void do_btree_send(BtreePoolSender thread, i_Asp dest, int[] row, int k,
            int owner) {

        if (use_threads) {
            if (use_thread_pool) {
                thread.put(dest, row, k, owner);
            } else {
                new BtreeSender(dest, row, k, owner).start();
            }
        } else {
            try {
                dest.btree_transfer(row, k, owner);
            } catch (Exception e) {
                System.out.println(rank + "btree transfer failed ! " + e);
                e.printStackTrace();
            }
        }
    }

    public void btree_transfer(int[] row, int k, int owner)
            throws RemoteException {
        int rel = rel_rank(rank, owner, nodes);
        int left, right;

        left = ((rel + 1) * 2) - 1;
        right = left + 1;

        if (left < nodes) {
            // send to left.
            int dest = abs_rank(left, owner, nodes);
            do_btree_send(leftSender, receivers[dest], row, k, owner);
        }

        if (right < nodes) {
            // send to right.
            int dest = abs_rank(right, owner, nodes);
            do_btree_send(rightSender, receivers[dest], row, k, owner);
        }

        synchronized (this) {
            tab[k] = row;
            notifyAll();
        }
    }

    void btree_bcast(int k, int owner) {

        try {
            btree_transfer(tab[k], k, owner);
        } catch (Exception e) {
            System.out.println("Btree transfer failed ! " + e);
            e.printStackTrace();
        }
    }

    //----------------------------------------------------------------------------

    void get_bounds() {
        int nlarge, nsmall;
        int size_large, size_small;

        nlarge = n % nodes;
        nsmall = nodes - nlarge;

        size_small = n / nodes;
        size_large = size_small + 1;

        if (rank < nlarge) { /* I'll  have a large chunk */
            lb = rank * size_large;
            ub = lb + size_large;
        } else {
            lb = nlarge * size_large + (rank - nlarge) * size_small;
            ub = lb + size_small;
        }

        //    System.out.println(rank + "[" + lb + " ... " + ub + "]"); 
    }

    int owner(int k) {
        int nlarge, nsmall;
        int size_large, size_small;

        nlarge = n % nodes;
        nsmall = nodes - nlarge;
        size_small = n / nodes;
        size_large = size_small + 1;

        if (k < (size_large * nlarge)) {
            return (k / size_large);
        } else {
            return (nlarge + (k - (size_large * nlarge)) / size_small);
        }
    }

    void init_tab() {
        int i, j;

        tab = new int[n][n];
        OrcaRandom r = new OrcaRandom();

        for (i = 0; i < n; i++) {
            if (lb <= i && i < ub) { /* my partition */
                for (j = 0; j < n; j++) {
                    tab[i][j] = (i == j ? 0 : r.val() % 1000);
                }
            } else {
                tab[i] = null;
            }
        }
    }

    void do_asp() {
        int i, j, k, tmp, nrows;

        nrows = ub - lb;

        for (k = 0; k < n; k++) {
            // if(rank == 0) {
            //	System.out.print(".");
            //	System.out.flush();
            // }

            bcast(k, owner(k)); // Owner of k sends tab[k] to al others, 
            // which receive it in tab[k].
            for (i = lb; i < ub; i++) {
                if (i != k) {
                    for (j = 0; j < n; j++) {
                        tmp = tab[i][k] + tab[k][j];
                        if (tmp < tab[i][j]) {
                            tab[i][j] = tmp;
                        }
                    }
                }
            }
        }
    }

    void print_table() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                System.out.print(tab[i][j] + " ");
            }
            System.out.println();
        }
    }

    /******************** Main program *************************/

    public void start() {
        long start, end;

        if (rank == 0) {
            System.out.println("Asp/manta started, n = " + n);
        }

        try {
            global.start();
        } catch (Exception e) {
            System.out.println("global.start() failed ! " + e);
            e.printStackTrace();
        }

        start = System.currentTimeMillis();

        do_asp();

        try {
            global.done();
        } catch (Exception e) {
            System.out.println("global.done() failed ! " + e);
            e.printStackTrace();
        }

        end = System.currentTimeMillis();

        double time = end - start;

        if (rank == 0) {
            System.out.println("\nAsp/manta "
                    + (use_threads ? " with threads, " : "no threads, ")
                    + (use_thread_pool ? "with threadpool, "
                            : "no threadpool, ") + "took " + (time / 1000.0)
                    + " secs.");

            if (print_result)
                print_table();
            info.printTime("Asp, " + n + "x" + n, end - start);
        }
    }

}