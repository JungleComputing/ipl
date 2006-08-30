/* $Id$ */

public final class Mtdf extends ibis.satin.SatinObject implements
        MtdfInterface, java.io.Serializable {
    static final int INF = 10000;

    static TranspositionTable tt = new TranspositionTable();

    static final boolean BEST_FIRST = true;

    static final boolean DO_ABORT = true;

    public void spawn_depthFirstSearch(NodeType node, int pivot, int depth,
            short currChild) throws Done {
        depthFirstSearch(node, pivot, depth);
        throw new Done(node.score, currChild);
    }

    NodeType depthFirstSearch(NodeType node, int pivot, int depth) {
        NodeType children[];
        short bestChild = 0;
        TranspositionTableEntry e;
        short currChild = 0;

        // System.out.println("depthFirstSearch: pivot = " + pivot
        //         + ", depth = " + depth);

        tt.visited++;

        if (depth == 0 || (children = node.generateChildren()) == null) {
            node.evaluate();
            return null;
        }

        e = tt.lookup(node.signature);
        if (e != null && node.signature == e.tag) {
            tt.sorts++;
            if (e.depth >= depth) { // watch out with equal sign!
                if ((e.lowerBound ? e.value >= pivot : e.value < pivot)) {
                    tt.hits++;
                    node.score = e.value;
                    // System.out.println("tt hit: node.score = " + node.score);
                    return children[e.bestChild];
                }
            }

            currChild = e.bestChild;
        }

        node.score = -INF;
        if (BEST_FIRST) {
            // do first child myself, if it generates a cut-off, stop.
            depthFirstSearch(children[currChild], 1 - pivot, depth - 1);

            if (-children[currChild].score > node.score) {
                tt.scoreImprovements++;
                bestChild = currChild;
                node.score = (short) -children[currChild].score;

                if (node.score >= pivot) {
                    tt.cutOffs++;
                    // update transposition table
                    tt.store(node.signature, node.score, currChild,
                            (byte) depth, node.score >= pivot);
                    return children[currChild];
                }
            }
        }

        boolean aborted = false;

        for (short i = (short) (children.length - 1); i >= 0; i--) {
            if (BEST_FIRST && i == currChild)
                continue;

            try {
                spawn_depthFirstSearch(children[i], 1 - pivot, depth - 1, i);
            } catch (Done d) {
                // System.out.println("Caught Done, -d.score = " + (-d.score)
                //         + ", node.score = " + node.score);
                if (-d.score > node.score) {
                    tt.scoreImprovements++;
                    bestChild = d.currChild;
                    node.score = (short) -d.score;

                    if (DO_ABORT && node.score >= pivot) {
                        // System.out.println("Abort ...");
                        aborted = true;
                        abort();
                    }
                }
                return null;
            }
            /*
             if (aborted) {
             System.out.println("Saw aborted set");
             break;
             }
             */
        }

        sync();

        // update transposition table
        tt.store(node.signature, node.score, bestChild, (byte) depth,
                node.score >= pivot);
        return children[bestChild];
    }

    NodeType mtdf(NodeType root, int depth) {
        int lowerBound = -INF;
        int upperBound = INF;
        int pivot = 0;
        NodeType bestChild;

        do {
            System.out.print(pivot == 0 ? "[" : "|");
            System.out.print(pivot);
            System.out.flush();

            bestChild = depthFirstSearch(root, pivot, depth);

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

    static NodeType doMtdf(NodeType root, int depth) {
        Mtdf m = new Mtdf();
        return m.mtdf(root, depth);
    }

    public static void main(String[] args) {
        Main.do_main(args);
    }
}
