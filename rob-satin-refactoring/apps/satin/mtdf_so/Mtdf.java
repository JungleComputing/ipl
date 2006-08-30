/* $Id$ */


import java.rmi.*;

public final class Mtdf extends ibis.satin.SatinObject implements
        MtdfInterface, java.io.Serializable {
    static final int THRESHOLD = 9;

    static final boolean BEST_FIRST = true;

    static final boolean DO_ABORT = true;

    static final int INF = 10000;

    NodeType root = null;

    public void spawn_depthFirstSearch(NodeType node, int pivot, int depth,
            short currChild, TranspositionTable tt) throws Done {
	root = node;
        depthFirstSearch(node, pivot, depth, tt);
        throw new Done(node.score, currChild);
    }

    NodeType depthFirstSearch(NodeType node, int pivot, int depth, TranspositionTable tt) {
        NodeType children[];
        short bestChild = 0;
        short currChild = 0;
        TTEntry e = null;

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
                    return children[e.bestChild];
                }
            }

            currChild = e.bestChild;
        }

        node.score = -INF;
        if (BEST_FIRST) {
            // do first child myself, if it generates a cut-off, stop.
            depthFirstSearch(children[currChild], 1 - pivot, depth - 1, tt);

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

        if (depth > THRESHOLD) {
            for (short i = (short) (children.length - 1); i >= 0; i--) {
                if (BEST_FIRST && i == currChild)
                    continue;

                try {
                    spawn_depthFirstSearch(children[i], 1 - pivot, depth - 1, i, tt);
                } catch (Done d) {
                    if (-d.score > node.score) {
                        tt.scoreImprovements++;
                        bestChild = d.currChild;
                        node.score = (short) -d.score;

                        if (DO_ABORT && node.score >= pivot) {
                            abort();
                        }
                    }
                    return null; // exit inlet thread
                }
            }

            sync();
        } else {
            for (short i = 0; i < children.length; i++) {
                if (BEST_FIRST && i == currChild)
                    continue;

                depthFirstSearch(children[i], 1 - pivot, depth - 1, tt);

                if (-children[i].score > node.score) {
                    tt.scoreImprovements++;
                    bestChild = i;
                    node.score = (short) -children[i].score;

                    if (/*DO_ABORT &&*/node.score >= pivot) {
                        break;
                    }
                }
            }

        }

        // update transposition table
        tt.store(node.signature, node.score, bestChild, (byte) depth,
                node.score >= pivot);
        return children[bestChild];
    }

    NodeType mtdf(NodeType root, int depth, TranspositionTable tt) {
        int lowerBound = -INF;
        int upperBound = INF;
        int pivot = 0;
        NodeType bestChild;

        do {
            System.out.print(pivot == 0 ? "[" : "|");
            System.out.print(pivot);
            System.out.flush();

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

    static NodeType doMtdf(NodeType root, int depth, TranspositionTable tt) {
        Mtdf m = new Mtdf();
	ObjectMeter om = new ObjectMeter();
	NodeType n = null;
	System.err.println("max,free memory: " + Runtime.getRuntime().maxMemory() + "," 
			   + Runtime.getRuntime().freeMemory());
	try {
	    n = m.mtdf(root, depth, tt);
	    
	} catch (OutOfMemoryError e) {
	    System.err.println("transposition table size: " + om.getObjectSize(tt));
	    System.err.println("max,free memory: " + Runtime.getRuntime().maxMemory() + "," 
			       + Runtime.getRuntime().freeMemory());
	}
	return n;
    }

    public static void main(String[] args) {
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
}
