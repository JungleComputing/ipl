public final class Mtdf extends ibis.satin.SatinObject implements MtdfInterface, java.io.Serializable {
	static final int INF = 10000;
	static TranspositionTable tt = new TranspositionTable();
	
	public void spawn_depthFirstSearch(NodeType node, int pivot, int depth, short currChild) throws Done {
		depthFirstSearch(node, pivot, depth);
		throw new Done(node.score, currChild);
	}

	NodeType depthFirstSearch(NodeType node, int pivot, int depth) {
		NodeType children[];
		short bestChild = 0;
		TranspositionTableEntry e;
		short[] order;

		tt.visited++;

		if(depth == 0 || (children = node.generateChildren()) == null) {
			node.evaluate();
			return null;
		}

		e = tt.lookup(node.signature);
		if(e != null && node.signature == e.tag) {
			tt.sorts++;
			if(e.depth >= depth) {
				if((e.lowerBound ? e.value >= pivot : e.value < pivot)) {
					tt.hits++;
					node.score = e.value;
					return children[e.bestChild];
				}
			}
			
			order = new short[children.length];
			order[0] = e.bestChild;
			for(short i=0; i<e.bestChild; i++) {
				order[i+1] = i;
			}
			for(short i=(short) (e.bestChild+1); i<children.length; i++) {
				order[i] = i;
			}
		} else {
			order = new short[children.length];
			for(short i=0; i<children.length; i++) {
				order[i] = i;
			}
		}

		node.score = -INF;

		// do first child myself, if it generates a cutt-off, stop.
		depthFirstSearch(children[order[0]], 1-pivot, depth - 1);

		boolean cutOff = false;
		short currChild = order[0];
		if (-children[currChild].score > node.score) {
			tt.scoreImprovements++;
			bestChild = currChild;
			node.score = (short) -children[currChild].score;
			
			if (node.score >= pivot) {
				cutOff = true;
				tt.aborts++;
			}
		}

		if(!cutOff) {
//			for (short i = 1; i < children.length; i++) {
			for (short i = (short)(children.length-1); i >= 1; i--) {
				currChild = order[i];
				try {
					spawn_depthFirstSearch(children[currChild], 1-pivot, depth - 1, currChild);
				} catch (Done d) {
//					System.out.println("in catch, my score = " + node.score + ", spawned score = " + d.score);
					if (-d.score > node.score) {
						tt.scoreImprovements++;
						bestChild = d.currChild;
						node.score = (short) -d.score;
						
						if (node.score >= pivot) {
							tt.aborts++;
							abort();
						}
					}
					return null;
				}
			}
			sync();
		}

		// update transposition table
		e = new TranspositionTableEntry();
		e.tag = node.signature;
		e.value = node.score;
		e.bestChild = bestChild;
		e.depth = (byte) depth;
		e.lowerBound = node.score >= pivot;
		tt.store(e);
		
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
