// $Id$
//
// Clause weighting method that uses the inverse cube of the
// clause size as size.

class CubeClauseReviewer extends ClauseReviewer {
    /**
     * Given a number of choices in a clause, returns the information
     * content of this clause.
     * @param n The number of choices.
     * @return The information contents of this clause.
     */
    float info( int n )
    {
	if( n == 0 ){
	    n = 1;
	}
        float res = 1.0f/(n*n*n);
	return res;
    }

}
