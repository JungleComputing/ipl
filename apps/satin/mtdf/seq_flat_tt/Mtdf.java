public final class Mtdf {
    static final int INF = 10000;

    static TranspositionTable tt = new TranspositionTable();

    static final boolean BEST_FIRST = true;

    static final boolean DO_ABORT = true;

    NodeType depthFirstSearch(NodeType node, int pivot, int depth) {
        NodeType children[];
        short bestChild = 0;
        short currChild = 0;
        int ttIndex;

        tt.visited++;

        if (depth == 0 || (children = node.generateChildren()) == null) {
            node.evaluate();
            return null;
        }

        ttIndex = tt.lookup(node.signature);
        if (ttIndex != -1 && node.signature == tt.tags[ttIndex]) {
            tt.sorts++;
            if (tt.depths[ttIndex] >= depth) { // watch out with equal sign!
                if ((tt.lowerBounds[ttIndex] ? tt.values[ttIndex] >= pivot
                        : tt.values[ttIndex] < pivot)) {
                    tt.hits++;
                    node.score = tt.values[ttIndex];
                    return children[tt.bestChildren[ttIndex]];
                }
            }

            currChild = tt.bestChildren[ttIndex];
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

        for (short i = 0; i < (short) children.length; i++) {
            if (BEST_FIRST && i == currChild)
                continue;

            depthFirstSearch(children[i], 1 - pivot, depth - 1);

            if (-children[i].score > node.score) {
                tt.scoreImprovements++;
                bestChild = i;
                node.score = (short) -children[i].score;

                if (node.score >= pivot) {
                    tt.cutOffs++;
                    // update transposition table
                    tt.store(node.signature, node.score, i, (byte) depth,
                            node.score >= pivot);
                    return children[i];
                }
            }
        }

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