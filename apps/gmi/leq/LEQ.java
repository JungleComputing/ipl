
import java.io.*;

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

    public void start() {

        int phase, i_glob;
        double residue;

        double[] new_x, x_copy, b;
        double[][] a;

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
        //  RuntimeSystem.barrier();

        // Start the calculation.		
        System.out.println("LEQ on " + cpu + " starting calculation.");

        long time = System.currentTimeMillis();
        //		long t1 = 0, t2 = 0;

        do {
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

            x_val.do_set(new_x, residue);

        } while (!x_val.done());

        if (cpu == 0) {
            System.out.println("Stopped with residue " + x_val.residue
                    + " with bound " + Main.BOUND);
        }

        // RuntimeSystem.barrier();

        time = System.currentTimeMillis() - time;

        if (cpu == 0) {
            System.out.println("Time : " + (time / 1000.0) + " sec."
                    + " (phases: " + phase + ")");
            /*
             System.out.println("Solution vector : ");

             for (int i=0;i<1;i++) {
             if (x_val.subscr(i) != 0.0) {
             System.out.println("x[" + i + "] = " + x_val.subscr(i));
             }
             }		       		
             */
        }
    }
}

