/* $Id$ */

class Main {
    int depth;

    boolean verbose;

    String file;

    static {
        System.setProperty( "satin.tuplespace.multicast", "true" );
    }

    Main(int depth, boolean verbose, String file) {
        this.depth = depth;
        this.verbose = verbose;
        this.file = file;
    }

    void search() {
        AwariBoard root;
        AwariBoard bestChild = null;

        if (file == null) {
            root = AwariBoard.getRoot();
        } else {
            root = AwariBoard.readBoard(file);
        }

        System.out.println("searching with root: ");
        root.print();

        long start = System.currentTimeMillis();
        for (int d = 1; d <= depth; d += 2) {
            System.out.println("depth is now: " + d);
            bestChild = (AwariBoard) Mtdf.doMtdf(root, d);
        }
        long end = System.currentTimeMillis();

        if (bestChild == null) {
            System.err.println("sukkel!");
            System.exit(1);
        }

        System.out.println("Best move: ");
        bestChild.mirror().print();
        System.out.println("application Awari (" + depth + ","
                + (file == null ? "start" : file) + ") took "
                + ((double) (end - start) / 1000.0) + " seconds");
    }
}
