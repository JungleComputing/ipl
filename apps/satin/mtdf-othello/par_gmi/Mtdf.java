/* $Id$ */

import ibis.gmi.*;

public final class Mtdf extends ibis.satin.SatinObject implements
        MtdfInterface, java.io.Serializable {
    static final int INF = 10000;

    static TranspositionTable tt;

    static final boolean BEST_FIRST = true;

    static final boolean DO_ABORT = true;

    static int pivot = 0;

    static int threshold =
            ibis.util.TypedProperties.intProperty("mtdf.threshold", 7);

    static void postSatinInit() {
        try {
            int size = Group.size();
            int rank = Group.rank();

            if (rank == 0)
                // The first node is to create the group
                Group.create("TT", TranspositionTableIntr.class, size);

            // Create a transposition table
            tt = new TranspositionTable(Main.getTagSize());

            // ..and join the group with this one updatable object
            Group.join("TT", tt);

            // All nodes use the group object to send updates
            TranspositionTableIntr group =
                    (TranspositionTableIntr)Group.lookup("TT");

            tt.setGroup(group);

            // In order to send the update, remoteStore() is to be used..
            GroupMethod method = Group.findMethod(group,
                    "void remoteStore(TranspositionTableWrapper)");

            // ..to broadcast the update to the whole group asynchronously
            method.configure(new GroupInvocation(), new DiscardReply());

            System.out.println(rank + ": done initializing GMI group");

        } catch (Exception e) {
            System.err.println("Error creating transposition table: " + e);
            System.exit(1);
        }
    }

    public void spawn_depthFirstSearch(NodeType node, int pivot, int depth,
            short currChild) throws Done {
        depthFirstSearch(node, pivot, depth);
        throw new Done(node.score, currChild);
    }

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
