public final class Mtdf {

    static final boolean BEST_FIRST = true;

    static final boolean DO_ABORT = true;

    static final int INF = 10000;

    static TranspositionTable tt = new TranspositionTable();

    static NodeType depthFirstSearch(NodeType node, int pivot, int depth) {
        NodeType children[];
        short bestChild = 0;
        TranspositionTableEntry e;
        short currChild = 0;

        tt.visited++;

        if (depth == 0 || (children = node.generateChildren()) == null) {
            node.evaluate();
            return null;
        }

        e = tt.lookup(node.signature);
        if (e != null && node.signature == e.tag) {
            tt.sorts++;
            if (e.depth >= depth) {
                if ((e.lowerBound ? e.value >= pivot : e.value < pivot)) {
                    tt.hits++;
                    node.score = e.value;
                    return children[e.bestChild];
                }
            }

            currChild = e.bestChild;
        }

        node.score = -INF;

        if (BEST_FIRST) {
            // try best child first, if it generates a cut-off, stop.
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

        for (short i = 0; i < children.length; i++) {
            if (BEST_FIRST && i == currChild)
                continue;

            depthFirstSearch(children[i], 1 - pivot, depth - 1);

            //			System.out.println("my score = " + node.score + ", spawned score = " + children[currChild].score);

            if (-children[i].score > node.score) {
                tt.scoreImprovements++;
                bestChild = i;
                node.score = (short) -children[i].score;

                if (DO_ABORT && node.score >= pivot) {
                    tt.aborts++;
                    break;
                }
            }
        }

        // update transposition table
        tt.store(node.signature, node.score, bestChild, (byte) depth,
                node.score >= pivot);

        return children[bestChild];
    }

    static NodeType mtdf(NodeType root, int depth) {
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
}