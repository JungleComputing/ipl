// $Id$

/**
 * The context of the solve method. The fields in this class are cloned
 * for every recursion of the solve() method.
 */
public class SATContext implements java.io.Serializable {
    /** The variables to assign to, in order of decreasing use count. */
    int varlist[];

    /** The number of open terms of each clause. */
    private int terms[];	/* The number of open terms of each clause. */

    /** The assignments to all variables. */
    int assignments[];

    /** Satisified flags for all clauses in the problem. */
    private boolean satisfied[];

    /** The number of unsatisfied clauses. */
    private int unsatisfied;

    /** Constructs a Context with the specified elements. */
    private SATContext( int vl[], int tl[], int al[], boolean sat[], int us ){
        varlist = vl;
	terms = tl;
	assignments = al;
	satisfied = sat;
	unsatisfied = us;
    }

    private static final boolean tracePropagation = false;

    /** Constructs an empty Context. */
    public SATContext( int clauseCount, int terms[] ){
	satisfied = new boolean[clauseCount];
	unsatisfied = clauseCount;
	this.terms = terms;
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

    /** Verifies that the term count of the specified clause is correct. */
    private void verifyTermCount( SATProblem p, int cno )
    {
	int termcount = 0;

        if( satisfied[cno] ){
	    // We don't care.
	}
	Clause c = p.clauses[cno];

	int arr[] = c.pos;

	// Now count the unsatisfied variables.
	for( int j=0; j<arr.length; j++ ){
	    int v = arr[j];

	    if( assignments[v] == 1 ){
		System.err.println( "Error: positive variable " + v + " of clause " + c + " has positive assignment, but clause is not satisfied"  );
		IntVector pos = p.getPosClauses( v );
		String verdict;

		if( pos.contains( cno ) ){
		    verdict = "yes";
		}
		else {
		    verdict = "no";
		}
		System.err.println( "       Does variable " + v + " list clause " + cno + " as positive occurence? " + verdict );
	    }
	    else if( assignments[v] == -1 ){
		// Count this unassigned variable.
		termcount++;
	    }
	}

	// Keep searching for the unassigned variable 
	arr = c.neg;
	for( int j=0; j<arr.length; j++ ){
	    int v = arr[j];

	    if( assignments[v] == 0 ){
		System.err.println( "Error: negative variable " + v + " of clause " + c + " has negative assignment, but clause is not satisfied"  );
		IntVector neg = p.getNegClauses( v );
		String verdict;

		if( neg.contains( cno ) ){
		    verdict = "yes";
		}
		else {
		    verdict = "no";
		}
		System.err.println( "       Does variable " + v + " list clause " + cno + " as negative occurence? " + verdict );
	    }
	    else if( assignments[v] == -1 ){
		termcount++;
	    }
	}
	if( termcount != terms[cno] ){
	    System.err.println( "Error: I count " + termcount + " unassigned variables in clause " + c + " but the administration says " + terms[cno] );
	}
    }

    /** Propagates any unit clauses in the problem.  */
    private int propagateUnitClauses( SATProblem p )
    {
	boolean sawEm = false;

	for( int i=0; i<terms.length; i++ ){
	    if( !satisfied[i] && terms[i] == 1 ){
	        Clause c = p.clauses[i];
		int arr[] = c.pos;
		int var = -1;

		sawEm = true;
		// Now search for the variable that isn't satisfied.
		for( int j=0; j<arr.length; j++ ){
		    int v = arr[j];

		    if( assignments[v] == -1 ){
		        if( var != -1 ){
			    System.err.println( "A unit clause with multiple unassigned variables???" );
			    return 0;
			}
		    }
		}
		if( var != -1 ){
		    // We have found the unassigned one, propagate it.
		    if( tracePropagation ){
		        System.err.println( "Propagating positive unit variable " + var + " from clause " + c );
		    }
		    int res = propagatePosAssignment( p, var );
		    if( res != 0 ){
			// The problem is now conflicting/satisfied, we're
			// done.
		        return res;
		    }
		}
		else {
		    // Keep searching for the unassigned variable 
		    arr = c.neg;
		    for( int j=0; j<arr.length; j++ ){
			int v = arr[j];

			if( assignments[v] == -1 ){
			    if( var != -1 ){
				System.err.println( "A unit clause with multiple unassigned variables???" );
				return 0;
			    }
			}
		    }
		    if( var != -1 ){
			// We have found the unassigned one, propagate it.
			if( tracePropagation ){
			    System.err.println( "Propagating negative unit variable " + var + " from clause " + c );
			}
			int res = propagatePosAssignment( p, var );
			if( res != 0 ){
			    // The problem is now conflicting/satisfied, we're
			    // done.
			    return res;
			}
		    }
		}
	    }
	}
	if( !sawEm ){
	    System.err.println( "The promised unit clauses could not be found" );
	}
	return 0;
    }

    /**
     * Registers the fact that the specified clause is satisfied.
     * Returns wether the problem now contains unipolar variables.
     */
    private void markClauseSatisfied( SATProblem p, int cno )
    {
        if( !satisfied[cno] ){
	    unsatisfied--;
	    if( tracePropagation ){
	        System.err.println( "Clause " + p.clauses[cno] + " is now satisfied, " + unsatisfied + " to go" );
	    }
	}
	satisfied[cno] = true;
    }

    /**
     * Propagates the fact that variable 'var' is true.
     * @return -1 if the problem is now in conflict, 1 if the problem is now satisified, or 0 otherwise
     */
    public int propagatePosAssignment( SATProblem p, int var )
    {
        assignments[var] = 1;
	boolean hasUnitClauses = false;

	if( tracePropagation ){
	    System.err.println( "Propagating assignment var[" + var + "]=true" );
	}
	// Deduct this clause from all clauses that contain this as a
	// negative term.
	IntVector neg = p.getNegClauses( var );
	int sz = neg.size();
	for( int i=0; i<sz; i++ ){
	    int cno = neg.get( i );

	    terms[cno]--;
	    if( terms[cno] == 0 ){
		// We now have a term that cannot be satisfied. Conflict.
		if( tracePropagation ){
		    System.err.println( "Clause " + p.clauses[cno] + " conflicts with var[" + var + "]=true" );
		}
	        return -1;
	    }
	    else if( terms[cno] == 1 ){
		// Remember that we saw a unit clause, but don't
		// propagate it yet, since the administration is inconsistent.
		hasUnitClauses = true;
	    }
	}

	// Mark all clauses that contain this variable as a positive
	// term as satisfied.
	IntVector pos = p.getPosClauses( var );
	sz = pos.size();
	for( int i=0; i<sz; i++ ){
	    int cno = pos.get( i );

	    markClauseSatisfied( p, cno );
	}
	if( unsatisfied == 0 ){
	    // All clauses are now satisfied, we have a winner!
	    return 1;
	}

	// Now propagate unit clauses if there are any.
	if( hasUnitClauses ){
	    propagateUnitClauses( p );
	}
	return 0;
    }

    /** Propagates the fact that variable 'var' is false. */
    public int propagateNegAssignment( SATProblem p, int var )
    {
        assignments[var] = 0;
	boolean hasUnitClauses = false;

	if( tracePropagation ){
	    System.err.println( "Propagating assignment var[" + var + "]=false" );
	}
	// Deduct this clause from all clauses that contain this as a
	// Positive term.
	IntVector pos = p.getPosClauses( var );
	int sz = pos.size();
	for( int i=0; i<sz; i++ ){
	    int cno = pos.get( i );

	    terms[cno]--;
	    if( terms[cno] == 0 ){
		// We now have a term that cannot be satisfied. Conflict.
		if( tracePropagation ){
		    System.err.println( "Clause " + p.clauses[cno] + " conflicts with var[" + var + "]=false" );
		}
	        return -1;
	    }
	    else if( terms[cno] == 1 ){
		// Remember that we saw a unit clause, but don't
		// propagate it yet, since the administration is inconsistent.
		hasUnitClauses = true;
	    }
	}

	// Mark all clauses that contain this variable as a negative
	// term as satisfied.
	IntVector neg = p.getNegClauses( var );
	sz = neg.size();
	for( int i=0; i<sz; i++ ){
	    int cno = neg.get( i );

	    markClauseSatisfied( p, cno );
	}
	if( unsatisfied == 0 ){
	    // All clauses are now satisfied, we have a winner!
	    return 1;
	}

	// Now propagate unit clauses if there are any.
	if( hasUnitClauses ){
	    propagateUnitClauses( p );
	}
	return 0;
    }
}
