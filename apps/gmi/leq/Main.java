
import java.io.*;
import ibis.gmi.*;

class Main {

    public static final int DEFAULT_N = 180;

    public static final double BOUND = 0.001;

    public static void main(String args[]) {

        try {

            int n = DEFAULT_N;
            int piece, offset, counter;

            long start, end;

            switch (args.length) {
            case 0:
                n = DEFAULT_N;
                break;
            case 1:
                n = Integer.parseInt(args[0]);
                break;
            default:
                System.err.println("Usage: LEQ <N>");
                System.exit(1);
            }

            offset = n;

            int cpu = Group.rank();
            int cpus = Group.size();

            int size = n / cpus;
            int leftover = n % cpus;
            offset = cpu * size;

            if (cpu < leftover) {
                offset += cpu;
                size++;
            } else {
                offset += leftover;
            }

            if (cpu == 0) {
                Group.create("LEQ", LEQGroup.class, cpus);
            }

            DoubleVector x_val = new DoubleVector(cpus, n);
            Group.join("LEQ", x_val);

            LEQGroup g = (LEQGroup) Group.lookup("LEQ");
            GroupMethod m1 = Group.findMethod(g, "void set(double[], double)");
            m1.configure(new CombinedInvocation("LEQ-GATHER", cpu, cpus,
                    new MyCombiner(n), new GroupInvocation()),
                    new DiscardReply());
            x_val.init(g);

            new LEQ(cpu, cpus, x_val, offset, size, n).start();

            Group.exit();

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

}