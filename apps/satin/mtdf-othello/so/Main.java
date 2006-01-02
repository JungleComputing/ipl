class Main {
    int depth;

    String file;

    Main(int depth, String file) {
        this.depth = depth;
        this.file = file;
    }

    public static int getTagSize() {
        return OthelloBoard.getTagSize();
    }

    public static void do_main(String[] args) {
        int option = 0;
        int depth = 13;
        String file = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-f")) {
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
            System.err.println("Usage: java Mtdf [-f file] [depth]");
            System.exit(1);
        }

        Main m = new Main(depth, file);
        m.search();
    }

    void search() {
        OthelloBoard root;
        OthelloBoard bestChild = null;

        if (file == null) {
            root = OthelloBoard.getRoot();
        } else {
            root = OthelloBoard.readBoard(file);
        }

        System.out.println("searching with root: ");
        root.print();

        long start = System.currentTimeMillis();
        for (int d = 1; d <= depth; d += 2) {
            System.out.println("depth is now: " + d);
            bestChild = (OthelloBoard) Mtdf.doMtdf(root, d);
        }
        long end = System.currentTimeMillis();

        if (bestChild == null) {
            System.err.println("No result! Help!");
            System.exit(1);
        }

        System.out.println("Best move: ");
        bestChild.invert().print();

        System.out.println("application Othello (" + depth + ","
            + (file == null ? "start" : file) + ") took "
            + ((double) (end - start) / 1000.0) + " seconds");

        Mtdf.tt.stats();
    }
}
