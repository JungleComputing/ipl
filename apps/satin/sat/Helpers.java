// File: $Id$

/** Helper methods. */

class Helpers {
    static private final double log2 = Math.log( 2.0 );
    static private final int INFOCACHESZ = 100;
    static private final double infocache[] = new double[INFOCACHESZ];

    static {
        for( int i=0; i<INFOCACHESZ; i++ ){
            infocache[i] = calcInformation( i );
        }
    }

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
     * Given a number of choices, returns the information content of
     * this choice.
     * @param n the number of choices
     * @return the information contents of this choice.
     */
    static private double calcInformation( int n )
    {
        double info;

        if( n<2  ){
            info = Double.POSITIVE_INFINITY;
        }
        else {
            double p = 1/(double) n;
            info = (- p * Math.log( p ) - ((1.0-p)*Math.log( 1.0-p)) ) / log2;
        }
        return info;
    }

    /**
     * Given a number of choices, returns the information content of
     * this choice.
     * @param n the number of choices
     * @return the information contents of this choice.
     */
    static public float information( int n )
    {
        if( false ){
            if( n>=0 && n<INFOCACHESZ ){
                return infocache[n];
            }
            return calcInformation( n );
        }
        else {
            return 1/(float) (n*n*n);
        }
    }
}
