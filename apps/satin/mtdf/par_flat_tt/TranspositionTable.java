final class TranspositionTable {

	static final boolean SUPPORT_TT = true;

	static final int SIZE = 1 << 22;
	static int lookups = 0, hits = 0, sorts = 0, stores = 0, 
		overwrites = 0, visited = 0, scoreImprovements = 0, cutOffs = 0;

	static final byte LOWER_BOUND = 1;
	static final byte UPPER_BOUND = 2;
	static final byte EXACT_BOUND = 3;

	long[] tags = new long[SIZE]; // index bits are redundant...
	short[] values = new short[SIZE];
	short[] bestChildren = new short[SIZE];
	byte[] depths = new byte[SIZE];
	boolean[] lowerBounds = new boolean[SIZE];
	boolean[] valid = new boolean[SIZE];


	int lookup(long signature) {
		lookups++;
		if(SUPPORT_TT) {
			int index = (int)signature & (SIZE-1);
			if(valid[index]) return index;
			return -1;
		} else {
			return -1;
		}
	}

	void store(long tag, short value, short bestChild, byte depth, boolean lowerBound) {
		stores++;

		if(!SUPPORT_TT) return;

		int index = (int)tag & (SIZE-1);

		if(valid[index] && depth < depths[index]) {
			return;
		}

		overwrites++;

		tags[index] = tag;
		values[index] = value;
		bestChildren[index] = bestChild;
		depths[index] = depth;
		lowerBounds[index] = lowerBound;
		valid[index] = true;
	}

	protected void finalize() throws Throwable {
		System.out.println("tt: lookups: " + lookups + ", hits: " + hits + ", sorts: " + sorts +
				   ", stores: " + stores + ", overwrites: " + overwrites + 
				   ", score incs: " + scoreImprovements + 
				   ", cutoffs: " + cutOffs + ", visited: " + visited);
	}
}
