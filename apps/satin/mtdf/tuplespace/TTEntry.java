class TTEntry implements java.io.Serializable {
	long tag;
	short value;
	short bestChild;
	byte depth;
	boolean lowerBound;

	TTEntry (long tag, short value, short bestChild, byte depth, boolean lowerBound) {
	    this.tag = tag;
	    this.value = value;
	    this.bestChild = bestChild;
	    this.depth = depth;
	    this.lowerBound = lowerBound;
	}
    }
