// File: $Id$
//
// Helper methods.

public class Helpers {
    // Given an array 'a' and a size 'sz', create a new array of size 'sz'
    // that contains the first 'sz' elements of 'a'.
    static int[] cloneIntArray( int a[], int sz )
    {
        int res[] = new int[sz];

	System.arraycopy( a, 0, res, 0, sz );
	return res;
    }
}
