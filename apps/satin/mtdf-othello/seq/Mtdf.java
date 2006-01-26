/* $Id$ */

public final class Mtdf {
    static final int INF = 10000;

    static TranspositionTable tt = new TranspositionTable(Main.getTagSize()); 

    static final boolean BEST_FIRST = true;

    static final boolean DO_ABORT = true;

    static int pivot = 0;

    NodeType depthFirstSearch(NodeType node, int pivot, int depth) {
        NodeType children[];
        short bestChild = 0;
        short currChild = 0;
        int ttIndex;
        Tag tag;

        tt.visited++;

        if (depth == 0 || (children = node.generateChildren()) == null) {
            node.evaluate();
            return null;
        }

        tag = node.getTag();

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

                    tt.store(tag, node.score, currChild, (byte) depth,
                            node.score >= pivot);
                    return children[currChild];
                }
            }
        }

        for (short i = 0; i < (short)children.length; i++) {
            if (BEST_FIRST && i == currChild)
                continue;

            depthFirstSearch(children[i], 1 - pivot, depth - 1);

            if (-children[i].score > node.score) {
                tt.scoreImprovements++;
                bestChild = i;
                node.score = (short) -children[i].score;

                if (node.score >= pivot) {
                    tt.cutOffs++;

                    tt.store(tag, node.score, i, (byte) depth,
                            node.score >= pivot);
                    return children[i];
                }
            }
        }

        tt.store(tag, node.score, bestChild, (byte) depth,
                node.score >= pivot);
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
