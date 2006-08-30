/* $Id$ */

class LEQ {

    DoubleVector x_val;

    int size, offset, n, cpu;

    LEQ(int cpu, int cpus, DoubleVector x_val, int offset, int size, int n) {

        this.x_val = x_val;
        this.size = size;
        this.offset = offset;
        this.n = n;

        this.cpu = cpu;

        System.out.println("Created LEQ on " + cpu + " offset " + offset
                + " size " + size);
    }

    private void sync() {
        try {
            x_val.central.sync();
        } catch (Exception e) {
        }
    }

    public void start() {

        int phase, i_glob;
        double residue;

        double[] new_x, x_copy, b;
        double[][] a;

        long calc = 0;
        long comm = 0;

        phase = 0;

        a = new double[size][n];
        b = new double[size];
        new_x = new double[size];

        // Initialize the local data.

        for (int j = 0; j < size; j++) {
            for (int k = 0; k < n; k++) {
                a[j][k] = 1.0;
            }
            a[j][offset + j] = (double) n;
            b[j] = 1.0;
        }

        // Wait for the others.
        sync();

        // Start the calculation.		
        System.out.println("LEQ on " + cpu + " starting calculation.");

        long time = System.currentTimeMillis();

        long t1 = 0, t2 = 0;

        do {
            // Wait for the others.
            // sync();
            t1 = System.currentTimeMillis();

            if (phase > 0) {
                comm = comm + (t1 - t2);
            }

            residue = 0.0;
            phase++;

            x_copy = x_val.value();

            for (int i = 0; i < size; i++) {

                i_glob = i + offset;
                new_x[i] = b[i];

                for (int j = 0; j < i_glob; j++) {
                    new_x[i] -= a[i][j] * x_copy[j];
                }

                for (int j = i_glob + 1; j < n; j++) {
                    new_x[i] -= a[i][j] * x_copy[j];
                }

                new_x[i] = new_x[i] / a[i][i_glob];
                residue += Math.abs(new_x[i] - x_copy[i_glob]);
            }

            if (cpu == 0 && (phase % 100 == 0)) {
                long temp = System.currentTimeMillis();
                System.err.println("Phase = " + phase + " " + residue
                        + " time est. "
                        + (((temp - time) / (1000.0 * phase)) * 6906.0));
            }

            t2 = System.currentTimeMillis();

            calc = calc + (t2 - t1);

            x_val.set(offset, size, new_x, residue);

            //			System.out.println(cpu + " phase done " + residue);

        } while (!x_val.done(phase - 1));

        sync();

        time = System.currentTimeMillis() - time;

        if (cpu == 0) {
            System.out.println("Time            : " + (time / 1000.0) + " sec."
                    + " (phases: " + phase + ")");
            System.out.println(cpu + "calc = " + (calc) + " msec." + " comm = "
                    + comm + " msec");
            /*
             System.out.println("Solution vector : ");

             for (int i=0;i<1;i++) {
             if (x_val.subscr(i) != 0.0) {
             System.out.println("x[" + i + "] = " + x_val.subscr(i));
             }
             }		       		
             */
        } else {
            //			System.out.println("Done "+ cpu);
            System.out.println(cpu + "calc = " + (calc) + " msec." + " comm = "
                    + comm + " msec");
            System.exit(0);
        }
    }
}

