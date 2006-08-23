/**
 * You can get a version that only uses a local transposition table (the old "par" version)
 * by setting the mtdf.replicatedDepth larger than the search depth.
 */
import ibis.util.TypedProperties;

/* $Id$ */

public final class Mtdf extends ibis.satin.SatinObject implements MtdfInterface {

    static final boolean BEST_FIRST = TypedProperties.booleanProperty(
        "mtdf.bestfirst", true);

    static final boolean DO_ABORT = TypedProperties.booleanProperty(
        "mtdf.aborts", true);

    static final boolean SUPPORT_TT = TypedProperties.booleanProperty(
        "mtdf.tt", true);

    static final int REPLICATED_DEPTH = TypedProperties.intProperty(
        "mtdf.replicatedDepth", 7);

    static final int SPAWN_THRESHOLD = ibis.util.TypedProperties.intProperty(
        "mtdf.threshold", 7);

    static final int INF = 10000;

    transient TranspositionTable tt;

    transient int pivot = 0;

    Mtdf() {
        try {
            tt = new TranspositionTable();
            tt.exportObject();
        } catch (Exception e) {
            System.err.println("Error creating transposition table: "
                + e.getMessage());
            System.exit(1);
        }
    }

    public void spawn_depthFirstSearch(NodeType node, int pivot, int depth,
        short currChild, TranspositionTable tt) throws Done {
        depthFirstSearch(node, pivot, depth, tt);
        throw new Done(node.score, currChild);
    }

    NodeType depthFirstSearch(NodeType node, int pivot, int depth,
        TranspositionTable tt) {
        NodeType children[] = null;
        short bestChild = 0;
        short currChild = 0;
        int ttIndex = 0;
        Tag tag = null;

        tt.visited++;

        if (depth == 0 || (children = node.generateChildren()) == null) {
            node.evaluate();
            return null;
        }

        tag = node.getTag();

        // @@@ should maybe be sync, but satinc dies ??? --Rob

        //        synchronized (tt) {
        ttIndex = tt.lookup(tag);
        if (ttIndex != -1) {
            tt.sorts++;
            if (tt.depths[ttIndex] >= depth) {

                if ((tt.lowerBounds[ttIndex] ? tt.values[ttIndex] >= pivot
                    : tt.values[ttIndex] < pivot)) {
                    tt.hits++;
                    node.score = tt.values[ttIndex];
                    return children[tt.bestChildren[ttIndex]];
                }
            }

            currChild = tt.bestChildren[ttIndex];
        }
        //        }

        node.score = -INF;
        if (BEST_FIRST) {
            depthFirstSearch(children[currChild], 1 - pivot, depth - 1, tt);

            if (-children[currChild].score > node.score) {
                tt.scoreImprovements++;
                bestChild = currChild;
                node.score = (short) -children[currChild].score;

                if (node.score >= pivot) {
                    tt.cutOffs++;

                    tt.store(tag, node.score, currChild, (byte) depth,
                        node.score >= pivot);
                    return children[currChild];
                }
            }
        }
        if (depth > SPAWN_THRESHOLD) {
            for (short i = (short) (children.length - 1); i >= 0; i--) {
                if (BEST_FIRST && i == currChild) continue;

                try {
                    spawn_depthFirstSearch(children[i], 1 - pivot, depth - 1,
                        i, tt);
                } catch (Done d) {
                    if (-d.score > node.score) {
                        tt.scoreImprovements++;
                        bestChild = d.currChild;
                        node.score = (short) -d.score;

                        if (DO_ABORT && node.score >= pivot) {
                            abort();
                        }
                    }

                    return null;
                }
            }

            sync();
        } else {
            for (short i = 0; i < children.length; i++) {
                if (BEST_FIRST && i == currChild) continue;

                depthFirstSearch(children[i], 1 - pivot, depth - 1, tt);

                if (-children[i].score > node.score) {
                    tt.scoreImprovements++;
                    bestChild = i;
                    node.score = (short) -children[i].score;

                    if (node.score >= pivot) break;
                }
            }
        }

        tt.store(tag, node.score, bestChild, (byte) depth, node.score >= pivot);
        return children[bestChild];
    }

    NodeType mtdf(NodeType root, int depth) {
        int lowerBound = -INF;
        int upperBound = INF;
        boolean first = true;
        NodeType bestChild;

        do {
            System.out.print(first ? "[" : "|");
            System.out.print(pivot);
            System.out.flush();
            first = false;

            bestChild = depthFirstSearch(root, pivot, depth, tt);

            if (root.score < pivot) {
                upperBound = root.score;
                pivot = root.score;
            } else {
                lowerBound = root.score;
                pivot = root.score + 1;
            }
        } while (lowerBound < upperBound);

        System.out.println("]");

        return bestChild;
    }

    OthelloBoard search(OthelloBoard root, int depth) {
        OthelloBoard bestChild = null;

        for (int d = 1; d <= depth; d += 2) {
            System.out.println("depth is now: " + d);
            bestChild = (OthelloBoard) mtdf(root, d);
        }

        return bestChild;
    }

    public static void main(String[] args) {
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

        OthelloBoard root;
        if (file == null) {
            root = OthelloBoard.getRoot();
        } else {
            root = OthelloBoard.readBoard(file);
        }

        System.out.println("searching with root: ");
        root.print();

        Mtdf m = new Mtdf();

        long start = System.currentTimeMillis();
        OthelloBoard bestChild = m.search(root, depth);
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

    }
}
