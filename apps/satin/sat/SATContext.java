// $Id$

/**
 * The context of the solve method. The fields in this class are cloned
 * for every recursion of the solve() method.
 */
public class SATContext implements java.io.Serializable {
    /** The variables to assign to, in order of decreasing use count. */
    int varlist[];

    /** The number of open terms of each clause. */
    int terms[];	/* The number of open terms of each clause. */

    /** The assignments to all variables. */
    int assignments[];

    /** Satisified flags for all clausses in the problem. */
    boolean satisfied[];

    /** The number of still unsatisfied clauses. */
    int unsatisfied;

    /** Constructs a Context with the specified elements. */
    private SATContext( int vl[], int tl[], int al[], boolean sat[], int us ){
        varlist = vl;
	terms = tl;
	assignments = al;
	satisfied = sat;
	unsatisfied = us;
    }

    /** Constructs an empty Context. */
    SATContext( int clauseCount ){
	satisfied = new boolean[clauseCount];
	unsatisfied = clauseCount;
    }
        
    /** Returns a clone of Context. */
    public Object clone()
    {
        return new SATContext(
	    (int []) varlist.clone(),
	    (int []) terms.clone(),
	    (int []) assignments.clone(),
	    (boolean []) satisfied.clone(),
	    unsatisfied
	);
    }

    /**
     * Propagates the fact that variable 'var' is true.
     * @return -1 if the problem is now in conflict, 1 if the problem is now satisified, or 0 otherwise
     */
    public int propagatePosAssignment( SATProblem p, int var )
    {
        assignments[var] = 1;

	// Deduct this clause from all clauses that contain this as a
	// negative term.
	IntVector neg = p.getNegClauses( var );
	int sz = neg.size();
	for( int i=0; i<sz; i++ ){
	    int cno = neg.get( i );

	    if( !satisfied[cno] ){
	        unsatisfied--;
	    }
	    terms[cno]--;
	    if( terms[cno] == 0 ){
		// We now have a term that cannot be satisfied. Conflict.
	        return -1;
	    }
	}

	// Mark all clauses that contain this variable as a positive
	// term as satisfied.
	IntVector pos = p.getPosClauses( var );
	sz = pos.size();
	for( int i=0; i<sz; i++ ){
	    int cno = pos.get( i );

	    if( !satisfied[cno] ){
	        unsatisfied--;
	    }
	    satisfied[cno] = true;
	}
	if( unsatisfied == 0 ){
	    // All clauses are now satisfied, we have a winner!
	    return 1;
	}
	return 0;
    }

    /** Propagates the fact that variable 'var' is false. */
    public int propagateNegAssignment( SATProblem p, int var )
    {
        assignments[var] = 0;

	// Deduct this clause from all clauses that contain this as a
	// Positive term.
	IntVector neg = p.getPosClauses( var );
	int sz = neg.size();
	for( int i=0; i<sz; i++ ){
	    int cno = neg.get( i );

	    if( !satisfied[cno] ){
	        unsatisfied--;
	    }
	    terms[cno]--;
	    if( terms[cno] == 0 ){
		// We now have a term that cannot be satisfied. Conflict.
	        return -1;
	    }
	}

		// Mark all clauses that contain this variable as a negative
		// term as satisfied.
	IntVector pos = p.getNegClauses( var );
	sz = pos.size();
	for( int i=0; i<sz; i++ ){
	    int cno = pos.get( i );

	    if( !satisfied[cno] ){
	        unsatisfied--;
	    }
	    satisfied[cno] = true;
	}
	if( unsatisfied == 0 ){
	    // All clauses are now satisfied, we have a winner!
	    return 1;
	}
	return 0;
    }
}
