// File: $Id$

/** Helper methods. */

class Helpers {
    /**
     * Given an int array <code>a</code> and a size <code>sz</code>, create a new array of size <code>sz</code>
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
     * Given two int arrays, return true iff they have equal elements.
     */
    static boolean areEqualArrays( int a[], int b[] )
    {
        if( a.length != b.length ){
            return false;
        }
        for( int i=0; i<a.length; i++ ){
            if( a[i] != b[i] ){
                return false;
            }
        }
        return true;
    }

    static int[] append( int a[], int v )
    {
        int sz = a.length;
        int res[] = new int[sz+1];

	System.arraycopy( a, 0, res, 0, sz );
        res[sz] = v;
        return res;
    }

    static boolean contains( int a[], int v )
    {
	for( int i = 0; i < a.length; i++ ){
	    if( a[i] == v ){
		return true;
	    }
	}
	return false;
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
    public static void dumpAssignments( String label, byte assignment[] )
    {
	System.err.print( label + ":" );
	for( int j=0; j<assignment.length; j++ ){
	    byte v = assignment[j];
	    
	    if( v != -1 ){
		System.err.print( " v" + j + "=" + v );
	    }
	}
	System.err.println();
    }

}
