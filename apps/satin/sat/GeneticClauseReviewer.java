// $Id$
//
// Clause weighting method that uses a set of genes.

/**
 * This clause reviewer is programmed with the set of genes that is
 * given to it's constructor. Each gene represents the weight of
 * a clause of a given size. The weight for sizes 0 and 1 is fixed
 * at 1.0f, since these weights are never used anyway.
 */

class GeneticClauseReviewer extends ClauseReviewer {
    float weights[];
    ClauseReviewer filteredReviewer = new CubeClauseReviewer();

    public GeneticClauseReviewer( float genes[] ){
	weights = new float[genes.length+3];

	weights[0] = weights[1] = weights[2] = 1.0f;

	System.arraycopy( genes, 0, weights, 3, genes.length );
    }

    /**
     * Given a number of choices in a clause, returns the information
     * content of this clause.
     * @param n The number of choices.
     * @return The information contents of this clause.
     */
    float info( int n )
    {
	if( n>=weights.length ){
	    n = weights.length - 1;
	}
	return weights[n] * filteredReviewer.info( n );
    }

}
