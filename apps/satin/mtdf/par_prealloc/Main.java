/* $Id$ */

class Main {
    int depth;

    boolean verbose;

    String file;

    Main(int depth, boolean verbose, String file) {
        this.depth = depth;
        this.verbose = verbose;
        this.file = file;
    }

    public static void do_main(String[] args) {
        boolean verbose = false;
        int option = 0;
        int depth = 11;
        String file = null;

        for (int i = 0; i < args.length; i++) {
            if (false) {
            } else if (args[i].equals("-v")) {
                verbose = true;
            } else if (args[i].equals("-f")) {
                file = args[++i];
            } else if (option == 0) {
                depth = Integer.parseInt(args[i]);
                option++;
            } else {
                System.err.println("No such option: " + args[i]);
                System.exit(1);
            }
        }

        if (option > 1) {
            System.err.println("To many options, usage java Main [-v] [depth]");
            System.exit(1);
        }

        Main m = new Main(depth, verbose, file);
        m.search();
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
        bestChild.print();
        System.out.println("application Awari (" + depth + ","
                + (file == null ? "start" : file) + ") took "
                + ((double) (end - start) / 1000.0) + " seconds");
    }
}