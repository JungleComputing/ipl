// File: $Id$

/** Helper methods. */

class Helpers {
    /**
     * Given an array <code>a</code> and a size <code>sz</code>, create a new array of size <code>sz</code>
     * that contains the first <code>sz</code> elements of <code>a</code>.
     * @param a the array to clone
     * @param sz the number of elements to clone
     * @return the cloned array
     */
    static int[] cloneIntArray( int a[], int sz )
    {
        int res[] = new int[sz];

	System.arraycopy( a, 0, res, 0, sz );
	return res;
    }

    /**
     * Given a number of choices in a clause, returns the information
     * content of this choice.
     * @param n The number of choices.
     * @return The information contents of this choice.
     */
    static public float information( int n )
    {
	if( n == 0 ){
	    n = 1;
	}
        return 1.0f/(n*n*n);
    }

    /**
     * Given an array and an index range in that array, return the
     * index of the smallest element. If the range is empty,
     * return the first index in the range.
     * @param l the array
     * @param from the first element to consider
     * @param to the first element not to consider
     */
    private static int selectSmallest( int l[], int from, int to )
    {
	int index = from;

	for( int i = from; i<to; i++ ){
	    if( l[i]<l[index] ){
		index = i;
	    }
	}
	return index;
    }

    /**
     * Given an int array, sort it.
     * @param l The array to sort.
     */
    public static void sortIntArray( int l[] )
    {
	// This is insertion sort.
	int sz = l.length;
	
	// We don't have to sort the last element
	for( int i=0; i<sz-1; i++ ){
	    // Find the smallest.
	    int pos = selectSmallest( l, i, sz );

	    if( pos != i ){
		// Put the smallest in element i, and put the
		// current element in it's old position.
		int temp = l[i];
		l[i] = l[pos];
		l[pos] = temp;
	    }
	}
    }


    /**
     * Prints the specified array of assignments to the error stream.
     * @param assignment The array of assignments.
     */
    public static void dumpAssignments( String label, int assignment[] )
    {
	System.err.print( label + ": " );
	for( int j=0; j<assignment.length; j++ ){
	    int v = assignment[j];
	    
	    if( v != -1 ){
		System.err.print( " v" + j + "=" + v );
	    }
	}
	System.err.println();
    }

}
