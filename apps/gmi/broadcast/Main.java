
import ibis.gmi.*;

class Main {

    static final int COUNT = 100000;

    public static void main(String[] argv) {

        try {
            int nodes = Group.size();
            int rank = Group.rank();
            int count = COUNT;
            int num = 0;

            Data data = null;
            i_Data group = null;

            long start = 0;
            long end = 0;
            double best = 0.0;

            for (int i = 0; i < argv.length; i++) {
                if (false) {
                } else if (argv[i].equals("-count")) {
                    count = Integer.parseInt(argv[i + 1]);
                    i++;
                } else {
                    if (rank == 0) {
                        System.out.println("No such option: " + argv[i]);
                        System.out.println("usage : bench [-count N]");
                    }
                    System.exit(33);
                }
            }

            if (rank == 0) {
                Group.create("Data", i_Data.class, nodes);
            }

            data = new Data(count, nodes);
            Group.join("Data", data);

            group = (i_Data) Group.lookup("Data");

            GroupMethod m = Group.findMethod(group, "void foo()");
            m.configure(new SingleInvocation(0), new ReturnReply(0));

            m = Group.findMethod(group, "void invokeRep()");
            m.configure(new SingleInvocation(0), new DiscardReply());

            m = Group.findMethod(group, "void done(double)");
            m.configure(new SingleInvocation(0), new ReturnReply(0));

            m = Group.findMethod(group, "void barrier()");
            m.configure(new CombinedInvocation("bbb", rank, nodes,
                    new MyCombiner(), new SingleInvocation(0)),
                    new ReturnReply(0));

            if (data.myGroupRank == 0) {
                m = Group.findMethod(group, "void bar()");
                m.configure(new GroupInvocation(), new DiscardReply());
                data.init(group);
            }

            group.barrier();

            double result = 0.0;

            long uni;

            for (int r = 0; r < nodes; r++) {

                if (rank == r) {

                    System.out.println("Starting test on " + rank);

                    // first test gmi sync ucast latency
                    for (int i = 0; i < count; i++) {
                        group.foo();
                    }

                    start = System.currentTimeMillis();

                    for (int i = 0; i < count; i++) {
                        group.foo();
                    }

                    end = System.currentTimeMillis();

                    //double one_way = (1000.0*(end-start))/(10*count);
                    //one_way = one_way/2.0;

                    uni = (end - start) / 2;

                    //					System.out.println("one way lat = " + uni);

                    for (int i = 0; i < count; i++) {
                        group.invokeRep();
                        data.readBar(num++);
                    }

                    start = System.currentTimeMillis();

                    for (int i = 0; i < count; i++) {
                        group.invokeRep();
                        end = data.readBar(num++);
                    }

                    result = ((end - start) - uni) / ((double) count);

                    System.out.println("Result " + rank + ": " + result
                            + " msec/mcast (one_way: " + ((double) uni) / count
                            + " msec/ucast)");
                } else {
                    for (int i = 0; i < 2 * count; i++) {
                        data.readBar(num++);
                    }
                }
                group.barrier();
            }

            group.done(result);
            Group.exit();

        } catch (Exception e) {
            System.out.println("Oops " + e);
            e.printStackTrace();
        }
    }
}