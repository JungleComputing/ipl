// $Id$

/**
 * The context of the solve method. The fields in this class are cloned
 * for every recursion of the solve() method.
 */
public class SATContext implements java.io.Serializable {
    /** The number of open terms of each clause. */
    private int terms[];

    /** The assignments to all variables. */
    int assignments[];

    /** The number of positive clauses of each variable. */
    private int posclauses[];

    /** The number of negative clauses of each variable. */
    private int negclauses[];

    /** Satisified flags for all clauses in the problem. */
    private boolean satisfied[];

    /** The number of unsatisfied clauses. */
    public int unsatisfied;

    /**
     * Constructs a Context with the specified elements.
     */
    private SATContext(
	int tl[],
	int al[],
	int poscl[],
	int negcl[],
	boolean sat[],
	int us
    ){
	terms = tl;
	assignments = al;
	satisfied = sat;
	unsatisfied = us;
	posclauses = poscl;
	negclauses = negcl;
    }

    private static final boolean tracePropagation = false;
    private static final boolean doVerification = false;

    /**
     * Constructs an empty Context. 
     */
    public SATContext( int clauseCount, int terms[], int poscl[], int negcl[] ){
	satisfied = new boolean[clauseCount];
	unsatisfied = clauseCount;
	this.terms = terms;
	posclauses = poscl;
	negclauses = negcl;
    }
        
    /**
     * Returns a clone of this Context.
     * @return The clone.
     */
    public Object clone()
    {
        return new SATContext(
	    (int []) terms.clone(),
	    (int []) assignments.clone(),
	    (int []) posclauses.clone(),
	    (int []) negclauses.clone(),
	    (boolean []) satisfied.clone(),
	    unsatisfied
	);
    }

    /**
     * Verifies that the term count of the specified clause is correct.
     * @param p The SAT problem.
     * @param cno The clause to verify.
     */
    private void verifyTermCount( SATProblem p, int cno )
    {
	int termcount = 0;

        if( satisfied[cno] ){
	    // We don't care.
	    return;
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

	// Also count the negative variables.
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

    /**
     * Verifies that the clause counts of the specified variable are correct.
     * @param p The SAT problem.
     * @param var The variable to verify.
     */
    private void verifyClauseCount( SATProblem p, int var )
    {
	int poscount = 0;
	int negcount = 0;

	// Count the positive clauses
	IntVector pos = p.getPosClauses( var );
	int sz = pos.size();
	for( int i=0; i<sz; i++ ){
	    int cno = pos.get( i );

	    if( !satisfied[cno] ){
	        poscount++;
	    }
	}

	// Count the negative clauses
	IntVector neg = p.getNegClauses( var );
	sz = neg.size();
	for( int i=0; i<sz; i++ ){
	    int cno = neg.get( i );

	    if( !satisfied[cno] ){
	        negcount++;
	    }
	}
	if( posclauses[var] != poscount || negclauses[var] != negcount ){
	    System.err.println( "Error: clause count of var[" + var + "] says (" + posclauses[var] + "," + negclauses[var] + "), not (" + poscount + "," + negcount + ")" );
	    posclauses[var] = poscount;
	    negclauses[var] = negcount;
	}
    }

    /**
     * Propagates the specified unit clause.
     * @param p the SAT problem to solve
     * @param i the index of the unit clause
     * @return -1 if the problem is now in conflict, 1 if the problem is now satisified, or 0 otherwise
     */
    private int propagateUnitClause( SATProblem p, int i )
    {
	if( satisfied[i] ){
	    // Not interesting.
	    return 0;
	}
	Clause c = p.clauses[i];
	if( terms[i] != 1 ){
	    System.err.println( "Error: cannot propagate clause " + c + " since it's not a unit clause" );
	    return 0;
	}
	int arr[] = c.pos;
	boolean foundIt = false;
	if( tracePropagation ){
	    System.err.println( "Propagating unit clause " + c );
	}
	// Now search for the variable that isn't satisfied.
	for( int j=0; j<arr.length; j++ ){
	    int v = arr[j];

	    if( assignments[v] == -1 ){
		if( foundIt ){
		    System.err.println( "Error: a unit clause with multiple unassigned variables" );
		    return 0;
		}
		// We have found the unassigned one, propagate it.
		if( tracePropagation ){
		    System.err.println( "Propagating positive unit variable " + v + " from clause " + c );
		}
		int res = propagatePosAssignment( p, v );
		if( res != 0 ){
		    // The problem is now conflicting/satisfied, we're
		    // done.
		    return res;
		}
		foundIt = true;
	    }
	}

	// Keep searching for the unassigned variable 
	arr = c.neg;
	for( int j=0; j<arr.length; j++ ){
	    int v = arr[j];
	    if( assignments[v] == -1 ){
		if( foundIt ){
		    System.err.println( "Error: a unit clause with multiple unassigned variables" );
		    return 0;
		}
		// We have found the unassigned one, propagate it.
		if( tracePropagation ){
		    System.err.println( "Propagating negative unit variable " + v + " from clause " + c );
		}
		int res = propagateNegAssignment( p, v );
		if( res != 0 ){
		    // The problem is now conflicting/satisfied, we're
		    // done.
		    return res;
		}
		foundIt = true;
	    }
	}
	if( !satisfied[i] && !foundIt ){
	    System.err.println( "Error: unit clause " + c + " does not contain unassigned variables" );
	}
	return 0;
    }

    /**
     * Registers the fact that the specified clause is satisfied.
     * @return -1 if the problem is now in conflict, 1 if the problem is now satisified, or 0 otherwise
     */
    private int markClauseSatisfied( SATProblem p, int cno )
    {
	boolean hasUniPolar = false;

	if( satisfied[cno] ){
	    // Already marked as satisfied, nothing to do.
	    return 0;
	}
	satisfied[cno] = true;
	unsatisfied--;
	if( tracePropagation ){
	    System.err.println( "Clause " + p.clauses[cno] + " is now satisfied, " + unsatisfied + " to go" );
	}
	Clause c = p.clauses[cno];

	int pos[] = c.pos;
	int neg[] = c.neg;
	for( int i=0; i<pos.length; i++ ){
	    int var = pos[i];

	    posclauses[var]--;
	    if( doVerification ){
	        verifyClauseCount( p, var );
	    }
	    if( assignments[var] == -1 && posclauses[var] == 0 && negclauses[var] != 0 ){
		if( tracePropagation ){
		    System.err.println( "Variable " + var + " only occurs negatively (0," + negclauses[var] + ")"  );
		}
		// Only register the fact that there is an unipolar variable.
		// Don't propagate it yet, since the adminstration is
		// inconsistent.
		hasUniPolar = true;
	    }
	}
	for( int i=0; i<neg.length; i++ ){
	    int var = neg[i];

	    negclauses[var]--;
	    if( doVerification ){
	        verifyClauseCount( p, var );
	    }
	    if( assignments[var] == -1 && posclauses[var] != 0 && negclauses[var] == 0 ){
		if( tracePropagation ){
		    System.err.println( "Variable " + var + " only occurs positively (" + posclauses[var] + ",0)"  );
		}
		// Only register the fact that there is an unipolar variable.
		// Don't propagate it yet, since the adminstration is
		// inconsistent.
		hasUniPolar = true;
	    }
	}
	if( hasUniPolar ){
	    // Now propagate the unipolar variables.
	    for( int i=0; i<pos.length; i++ ){
		int var = pos[i];

		if( assignments[var] == -1 && posclauses[var] == 0 && negclauses[var] != 0 ){
		    int res = propagateNegAssignment( p, var );
		    if( res != 0 ){
			return res;
		    }
		}
	    }
	    for( int i=0; i<neg.length; i++ ){
		int var = neg[i];

		if( assignments[var] == -1 && posclauses[var] != 0 && negclauses[var] == 0 ){
		    int res = propagatePosAssignment( p, var );
		    if( res != 0 ){
			return res;
		    }
		}
	    }
	}
	return 0;
    }

    private void dumpAssignments()
    {
	System.err.print( "Assignments:" );
	for( int j=0; j<assignments.length; j++ ){
	    int v = assignments[j];
	    
	    if( v != -1 ){
		System.err.print( " v[" + j + "]=" + v );
	    }
	}
	System.err.println();
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
		// We now have a clause that cannot be satisfied. Conflict.
		if( tracePropagation ){
		    System.err.println( "Clause " + p.clauses[cno] + " conflicts with var[" + var + "]=true" );
		    dumpAssignments();
		}
	        return -1;
	    }
	    if( terms[cno] == 1 ){
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

	    int res = markClauseSatisfied( p, cno );
	    if( res != 0 ){
	        return res;
	    }
	}
	if( unsatisfied == 0 ){
	    // All clauses are now satisfied, we have a winner!
	    return 1;
	}

	// Now propagate unit clauses if there are any.
	if( hasUnitClauses ){
	    sz = neg.size();
	    for( int i=0; i<sz; i++ ){
		int cno = neg.get( i );

		if( doVerification ){
		    verifyTermCount( p, cno );
		}
		if( terms[cno] == 1 ){
		    int res = propagateUnitClause( p, cno );
		    if( res != 0 ){
			return res;
		    }
		}
	    }
	}
	return 0;
    }

    /**
     * Propagates the fact that variable 'var' is false.
     * @return -1 if the problem is now in conflict, 1 if the problem is now satisified, or 0 otherwise
     */
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
		// We now have a clause that cannot be satisfied. Conflict.
		if( tracePropagation ){
		    System.err.println( "Clause " + p.clauses[cno] + " conflicts with var[" + var + "]=false" );
		    dumpAssignments();
		}
	        return -1;
	    }
	    if( terms[cno] == 1 ){
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

	    int res = markClauseSatisfied( p, cno );
	    if( res != 0 ){
	        return res;
	    }
	}
	if( unsatisfied == 0 ){
	    // All clauses are now satisfied, we have a winner!
	    return 1;
	}

	// Now propagate unit clauses if there are any.
	if( hasUnitClauses ){
	    sz = pos.size();
	    for( int i=0; i<sz; i++ ){
		int cno = pos.get( i );

		if( doVerification ){
		    verifyTermCount( p, cno );
		}
		if( terms[cno] == 1 ){
		    int res = propagateUnitClause( p, cno );
		    if( res != 0 ){
			return res;
		    }
		}
	    }
	}
	return 0;
    }

    /**
     * Returns the best decision variable to branch on, or -1 if there is none.
     */
    public int getDecisionVariable()
    {
        // For the moment we return the variable that is used the most.
	int bestvar = -1;
	int bestusecount = 0;

	for( int i=0; i<assignments.length; i++ ){
	    if( assignments[i] != -1 ){
		// Already assigned, so not interesting.
	        continue;
	    }
	    int usecount = posclauses[i] + negclauses[i];
	    if( usecount>bestusecount ){
		// This is a better one.
	        bestvar = i;
		bestusecount = usecount;
	    }
	}
	return bestvar;
    }

    /**
     * Returns true iff the given variable is more frequently used as
     * positive variable than as negative variable.
     * @param var the variable
     */
    public boolean posDominant( int var )
    {
        return (posclauses[var]>negclauses[var]);
    }

    /**
     * Given a variable, returns the maximum number of clauses it will satisfy.
     * @param var the variable
     * @return the solve count
     */
    public int getSolveCount( int var )
    {
	if( posclauses[var]>negclauses[var] ){
	    return posclauses[var];
	}
	else {
	    return negclauses[var];
	}
    }

    /**
     * Optimize the problem by searching for and propagating all unit
     * clauses and unipolar variables that we can find.
     * @param p The SAT problem this is the context for.
     * @return -1 if the problem is now in conflict, 1 if the problem is now satisified, or 0 otherwise
     */
    public int optimize( SATProblem p )
    {
	// Search for and propagate unit clauses.
	for( int i=0; i<terms.length; i++ ){
	    if( terms[i] == 1 ){
		int res = propagateUnitClause( p, i );
		if( res != 0 ){
		    return res;
		}
	    }
	}
	// Search for and propagate unipolar variables.
	for( int i=0; i<assignments.length; i++ ){
	    if( assignments[i] != -1 || (posclauses[i] == 0 && negclauses[i] == 0) ){
		// Unused variable, not interesting.
		continue;
	    }
	    if( posclauses[i] == 0 ){
		int res = propagateNegAssignment( p, i );
		if( res != 0 ){
		    return res;
		}
	    }
	    else if( negclauses[i] == 0 ){
		int res = propagatePosAssignment( p, i );
		if( res != 0 ){
		    return res;
		}
	    }
	}
	return 0;
    }
}
