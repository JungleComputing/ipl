
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import ibis.util.PoolInfo;

public class RadixMaster extends UnicastRemoteObject implements
        RadixMasterInterface, Runnable {

    static final int MAX_PROCESSORS = 64;

    static final int MAX_RADIX = 4096;

    int radix, number_Of_Processors, num_Keys, workers_Assigned;

    boolean doStats, testResult, doPrint;

    Stats stat[];

    String[] slaves;

    int log2_Radix;

    int[] key_Partition;

    IntRow key_From;

    ProcArg proc;

    Parts[] parts;

    Barrier bar, bar1;

    PoolInfo d;

    SlaveSort this_cpus_slave;

    private int num_trees;

    private TreeInterface itrees[];

    private int num_parts;

    private PartsInterface iparts[];

    RadixMaster(PoolInfo d, int nrkeys, int nworkers, int radix, boolean stats,
            boolean test, boolean print) throws Exception {

        super();

        this.d = d;
        System.out.println("nworkers = " + nworkers);
        this.radix = radix;
        doPrint = print;
        doStats = stats;
        testResult = test;
        number_Of_Processors = nworkers;
        stat = new Stats[number_Of_Processors];
        num_Keys = nrkeys;
        log2_Radix = log2Radix();
        workers_Assigned = 0;
        key_Partition = new int[MAX_PROCESSORS + 1];
        partition();
        //try to bind with registry
        System.out.println("Binding RadixMaster in registry");
        RMI_init.bind("RadixMaster", this);
        System.out.println("Bound RadixMaster in registry");
        create();
    }

    public void run() {
        try {
            bar1.sync();
            bar1.sync();
            if (testResult)
                check();
            if (doStats)
                printStats();
            bar1.sync();
        } catch (Exception e) {
            System.out.println("Problem with run");
            e.printStackTrace();
        }
        reset();
    }

    public int log2Radix() {
        double d1 = Math.log(radix);
        double d2 = Math.log(2);
        double t = d1 / d2;
        int tmp = (int) (t);
        if ((double) tmp != t) {
            System.out.println(" Radix must be a power of 2");
            System.exit(1);
        }
        return tmp;
    }

    public void create() throws Exception {
        int[] sub;

        bar = new Barrier(number_Of_Processors);
        bar1 = new Barrier(number_Of_Processors + 1);
        slaves = new String[number_Of_Processors];
        key_From = IntRow.generateArray(num_Keys);
        parts = new Parts[number_Of_Processors];
        itrees = new TreeInterface[number_Of_Processors];
        iparts = new PartsInterface[number_Of_Processors];
        for (int i = 0; i < number_Of_Processors; i++) {
            sub = key_From.subArray(key_Partition[i], key_Partition[i + 1]);
            parts[i] = new Parts(sub);

        }
        proc = new ProcArg(num_Keys, radix, log2_Radix, key_Partition);
    }

    public void partition() {
        int quotient = 0, remainder = 0, sum_i = 0, sum_f = 0, p = 0;

        quotient = num_Keys / number_Of_Processors;
        remainder = num_Keys % number_Of_Processors;
        while (sum_i < num_Keys) {
            key_Partition[p] = sum_i;
            p++;
            sum_i += quotient;
            sum_f += remainder;
            sum_i += sum_f / number_Of_Processors;
            sum_f = sum_f % number_Of_Processors;
        }
        key_Partition[p] = num_Keys;
    }

    public synchronized Job get_Job(int id) throws RemoteException {
        Job job;

        job = new Job(proc, parts[id], slaves, number_Of_Processors);
        return job;
    }

    public synchronized int logon(String workerName) throws RemoteException {
        slaves[workers_Assigned] = workerName;
        workers_Assigned++;

        return workers_Assigned - 1;
    }

    public void sync() throws RemoteException {
        bar.sync();
    }

    public void sync2() throws RemoteException {
        bar1.sync();
    }

    public ProcArg params_Init(int id) {
        ProcArg proc;
        int[] keys;
        int key_Start = key_Partition[id];
        int key_Stop = key_Partition[id + 1];

        keys = new int[key_Stop - key_Start];
        for (int i = key_Start; i < key_Stop; i++) {
            keys[i - key_Start] = key_From.row[i];
        }
        proc = new ProcArg(num_Keys, radix, log2_Radix, key_Partition);
        return proc;
    }

    public void reset() {
        try {
            RMI_init.unbind("RadixMaster");
        } catch (Exception e) {
            System.out.println("failed to unbind master" + e.getMessage());
        }
    }

    public void check() throws Exception {
        int[] part, key_To;
        int m = 0;

        key_To = new int[num_Keys];
        for (int i = 0; i < number_Of_Processors; i++) {
            part = iparts[i].getPart();
            for (int l = 0; l < part.length; l++) {
                key_To[m] = part[l];
                m++;
            }
        }
        for (int i = 0; i < key_To.length - 1; i++) {
            if (key_To[i] > key_To[i + 1]) {
                System.out.println("Master: Checksum error " + i);
                System.exit(1);
            }
        }
        System.out.println("Everything ok");
    }

    public void putStats(int host, Stats stats) {
        stat[host] = stats;
    }

    public void printStats() {
        long maxt, maxHistogram, maxSort, maxPermute, maxMerge;
        long mint, minHistogram, minSort, minPermute, minMerge;

        maxt = mint = stat[0].totalTime;
        maxHistogram = minHistogram = stat[0].histogramTime;
        maxSort = minSort = stat[0].sortTime;
        maxPermute = minPermute = stat[0].permuteTime;
        maxMerge = minMerge = stat[0].mergeTime;

        for (int i = 1; i < number_Of_Processors; i++) {
            if (stat[i].totalTime > maxt) {
                maxt = stat[i].totalTime;
            }
            if (stat[i].totalTime < mint) {
                mint = stat[i].totalTime;
            }
            if (stat[i].histogramTime > maxHistogram) {
                maxHistogram = stat[i].histogramTime;
            }
            if (stat[i].histogramTime < minHistogram) {
                minHistogram = stat[i].histogramTime;
            }
            if (stat[i].sortTime > maxSort) {
                maxSort = stat[i].sortTime;
            }
            if (stat[i].sortTime < minSort) {
                minSort = stat[i].sortTime;
            }

            if (stat[i].mergeTime > maxMerge) {
                maxMerge = stat[i].mergeTime;
            }
            if (stat[i].mergeTime < minMerge) {
                minMerge = stat[i].mergeTime;
            }
            if (stat[i].permuteTime > maxPermute) {
                maxPermute = stat[i].permuteTime;
            }
            if (stat[i].permuteTime < minPermute) {
                minPermute = stat[i].permuteTime;
            }
        }
        System.out
                .println("#cpu  totaltime\t histogramtime\t sorttime\t mergetime\t permutetime");
        for (int j = 0; j < number_Of_Processors; j++) {
            System.out.print(j);
            System.out.print("\t");
            System.out.print(stat[j].totalTime);
            System.out.print("\t  ");
            System.out.print(stat[j].histogramTime);
            System.out.print("\t\t  ");
            System.out.print(stat[j].sortTime);
            System.out.print("\t\t  ");
            System.out.print(stat[j].mergeTime);
            System.out.print("\t\t  ");
            System.out.println(stat[j].permuteTime);
        }
        System.out.println();
        System.out.println("Min\t" + mint + "\t  " + minHistogram + "\t\t  "
                + minSort + "\t\t  " + minMerge + "\t\t  " + minPermute);
        System.out.println("Max\t" + maxt + "\t  " + maxHistogram + "\t\t  "
                + maxSort + "\t\t  " + maxMerge + "\t\t  " + maxPermute);
        d.printTime("Radix, #keys = " + num_Keys, (mint + maxt) / 2);
    }

    void print(int[] data) {

        int length = data.length;
        for (int i = 0; i < length; i++) {
            System.out.println(data[i]);
        }
    }

    public synchronized TreeInterface[] getTrees(TreeInterface tree, int cpunum)
            throws RemoteException {
        num_trees++;
        itrees[cpunum] = tree;
        if (num_trees == number_Of_Processors) {
            notifyAll();
        } else
            while (num_trees < number_Of_Processors) {
                try {
                    wait();
                } catch (Exception e) {
                    throw new RemoteException(e.toString());
                }
            }
        return itrees;
    }

    public synchronized PartsInterface[] getParts(PartsInterface part,
            int cpunum) throws RemoteException {
        num_parts++;
        iparts[cpunum] = part;
        if (num_parts == number_Of_Processors) {
            notifyAll();
        } else
            while (num_parts < number_Of_Processors) {
                try {
                    wait();
                } catch (Exception e) {
                    throw new RemoteException(e.toString());
                }
            }
        return iparts;
    }
}