class Misc {
	public static final int
		MAX_HOPS	= 4,	 // Search depth of master.
		MAX_JOBS	= 10000,
		MAX_CLIENTS	= 100;

	// Test if a given city is present on the given path.
	static final boolean present(int city, int hops, int[] path) {
		for(int i=hops; i>0; i--) {
			if(path[i] == city) return true;
		}

		return false;
	}
}
