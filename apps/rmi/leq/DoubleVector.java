class DoubleVector {

    int n;

    BroadcastObject bco;

    Update upd;

    int cpu;

    int cpus;

    i_Central central;

    public DoubleVector(int cpu, int cpus, int n, double r,
            BroadcastObject bco, i_Central central) {
        this.n = n;
        this.bco = bco;

        upd = new Update();
        upd.update = new double[n];

        this.cpu = cpu;
        this.cpus = cpus;
        this.central = central;
    }

    public double[] value() {
        return upd.update;
    }

    public double subscr(int i) {
        return upd.update[i];
    }

    public boolean done(int seqno) {

        if (cpus > 1) {
            bco.get(upd, seqno);
        }

        // System.out.println("Stop = "+ upd.stop);
        return upd.stop;
    }

    public void set(int offset, int size, double[] update, double residue) {

        if (cpus > 1) {
            try {
                central.put(offset, size, update, residue);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            System.arraycopy(update, 0, upd.update, 0, size);
            upd.stop = (residue < Main.BOUND);
        }
    }
}

