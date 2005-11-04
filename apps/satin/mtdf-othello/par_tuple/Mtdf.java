public final class Mtdf extends ibis.satin.SatinObject implements
        MtdfInterface, java.io.Serializable {
    static final int INF = 10000;

    static TranspositionTable tt = new TranspositionTable();

    static final boolean BEST_FIRST = true;

    static final boolean DO_ABORT = true;

    static int threshold =
            ibis.util.TypedProperties.intProperty("mtdf.threshold", 7);

    static int pivot = 0;

    public void spawn_depthFirstSearch(NodeType node, int pivot, int depth,
            short currChild) throws Done {
        depthFirstSearch(node, pivot, depth);
        throw new Done(node.score, currChild);
    }

    NodeType depthFirstSearch(NodeType node, int pivot, int depth) {
        NodeType children[];
        short bestChild = 0;
        short currChild = 0;
        TranspositionTableEntry ttEntry;
        Tag tag;

        tt.visited++;

        if (depth == 0 || (children = node.generateChildren()) == null) {
            node.evaluate();
            return null;
        }

        tag = node.getTag();

        ttEntry = tt.lookup(tag);
        if (ttEntry != null) {
            tt.sorts++;
            if (ttEntry.depth >= depth) {
                if ((ttEntry.lowerBound ? ttEntry.value >= pivot
                        : ttEntry.value < pivot)) {
                    tt.hits++;
                    node.score = ttEntry.value;
                    return children[ttEntry.bestChild];
                }
            }

            currChild = ttEntry.bestChild;
        }

        node.score = -INF;
        if (BEST_FIRST) {
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

        if (depth > threshold) {
            for (short i = (short)(children.length - 1); i >= 0; i--) {
                if (BEST_FIRST && i == currChild)
                    continue;

                try {
                    spawn_depthFirstSearch(children[i], 1 - pivot, depth - 1,
                        i);
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
        }
        else {
            for (short i = 0; i < children.length; i++) {
                if (BEST_FIRST && i == currChild)
                    continue;

                depthFirstSearch(children[i], 1 - pivot, depth - 1);

                if (-children[i].score > node.score) {
                    tt.scoreImprovements++;
                    bestChild = i;
                    node.score = (short) -children[i].score;

                    if (node.score >= pivot) break;
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
