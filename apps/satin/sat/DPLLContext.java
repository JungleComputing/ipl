// $Id$

/**
 * The context of the solve method. The fields in this class are cloned
 * for every recursion of the solve() method.
 */
public final class DPLLContext implements java.io.Serializable {
    /**
     * A symbolic name for the `unassigned' value in
     * the `assignment' array.
     */
    private static final int UNASSIGNED = -1;

    /** The number of open terms of each clause. */
    private int terms[];

    /** The assignments to all variables. */
    byte assignment[];

    /** The number of positive clauses of each variable. */
    private int posclauses[];

    /** The number of negative clauses of each variable. */
    private int negclauses[];

    /** The information of a positive assignment of each variable. */
    private float posinfo[];

    /** The information of a negative assignment of each variable. */
    private float neginfo[];

    /** Satisified flags for each clause in the problem. */
    private boolean satisfied[];

    /** The number of unsatisfied clauses. */
    public int unsatisfied;

    /**
     * Constructs a Context with the specified elements.
     */
    private DPLLContext(
	int tl[],
	byte al[],
	int poscl[],
	int negcl[],
	float posinfo[],
	float neginfo[],
	boolean sat[],
	int us
    ){
	terms = tl;
	assignment = al;
	satisfied = sat;
	unsatisfied = us;
	posclauses = poscl;
	negclauses = negcl;
        this.posinfo = posinfo;
        this.neginfo = neginfo;
    }

    private static final boolean tracePropagation = false;
    private static final boolean doVerification = false;
    private static final boolean propagatePureVariables = true;

    /**
     * Constructs a SAT context based on the given SAT problem.
     * @param p The SAT problem to create the context for.
     * @return the constructed context
     */
    public static DPLLContext buildDPLLContext( SATProblem p )
    {
        int cno = p.getClauseCount();
        return new DPLLContext(
            p.buildTermCounts(),
            p.buildInitialAssignments(),
            p.buildPosClauses(),
            p.buildNegClauses(),
            p.buildPosInfo(),
            p.buildNegInfo(),
            new boolean[cno],
            cno
        );
    }

    /**
     * Returns a clone of this Context.
     * @return The clone.
     */
    public Object clone()
    {
        return new DPLLContext(
	    (int []) terms.clone(),
	    (byte []) assignment.clone(),
	    (int []) posclauses.clone(),
	    (int []) negclauses.clone(),
	    (float []) posinfo.clone(),
	    (float []) neginfo.clone(),
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

	    if( assignment[v] == 1 ){
		int[] pos = p.getPosClauses( v );
		boolean verdict = Helpers.contains( pos, cno );

		System.err.println( "Error: positive variable " + v + " of clause " + c + " has positive assignment, but clause is not satisfied"  );
		System.err.println( "       Does variable " + v + " list clause " + cno + " as positive occurence? " + verdict );
	    }
	    else if( assignment[v] == UNASSIGNED ){
		// Count this unassigned variable.
		termcount++;
	    }
	}

	// Also count the negative variables.
	arr = c.neg;
	for( int j=0; j<arr.length; j++ ){
	    int v = arr[j];

	    if( assignment[v] == 0 ){
		int neg[] = p.getNegClauses( v );
		boolean verdict = Helpers.contains( neg, cno );

		System.err.println( "Error: negative variable " + v + " of clause " + c + " has negative assignment, but clause is not satisfied"  );
		System.err.println( "       Does variable " + v + " list clause " + cno + " as negative occurence? " + verdict );
	    }
	    else if( assignment[v] == UNASSIGNED ){
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
	int pos[] = p.getPosClauses( var );
	for( int i=0; i<pos.length; i++ ){
	    int cno = pos[i];

	    if( !satisfied[cno] ){
	        poscount++;
	    }
	}

	// Count the negative clauses
	int neg[] = p.getNegClauses( var );
	for( int i=0; i<neg.length; i++ ){
	    int cno = neg[i];

	    if( !satisfied[cno] ){
	        negcount++;
	    }
	}
	if( posclauses[var] != poscount || negclauses[var] != negcount ){
	    System.err.println( "Error: clause count of v" + var + " says (" + posclauses[var] + "," + negclauses[var] + "), not (" + poscount + "," + negcount + ")" );
	    posclauses[var] = poscount;
	    negclauses[var] = negcount;
	}
    }

    /**
     * Registers the fact that the specified clause is in conflict with
     * the current assignments. This method throws a
     * restart exception if that is useful.
     * @param p The SAT problem.
     * @param cno The clause that is in conflict.
     */
    private void analyzeConflict( SATProblem p, int cno, int var )
    {
        if( tracePropagation ){
            System.err.println( "Clause " + p.clauses[cno] + " conflicts with v" + var + "=" + assignment[var] );
            dumpAssignments();
        }
    }

    /**
     * Propagates the specified unit clause.
     * @param p the SAT problem to solve
     * @param i the index of the unit clause
     * @return CONFLICTING if the problem is now in conflict, SATISFIED if the problem is now satisified, or UNDETERMINED otherwise
     */
    private int propagateUnitClause( SATProblem p, int i )
    {
	if( satisfied[i] ){
	    // Not interesting.
	    return SATProblem.UNDETERMINED;
	}
	Clause c = p.clauses[i];
        if( doVerification ){
            if( terms[i] != 1 ){
                System.err.println( "Error: cannot propagate clause " + c + " since it's not a unit clause" );
                return SATProblem.UNDETERMINED;
            }
        }
	int arr[] = c.pos;
	if( tracePropagation ){
	    System.err.println( "Propagating unit clause " + c );
	}
	// Now search for the variable that isn't satisfied.
	for( int j=0; j<arr.length; j++ ){
	    int v = arr[j];

	    if( assignment[v] == UNASSIGNED ){
		// We have found the unassigned one, propagate it.
		if( tracePropagation ){
		    System.err.println( "Propagating positive unit variable " + v + " from clause " + c );
		}
		int res = propagatePosAssignment( p, v );
		if( (res != 0) || !doVerification ){
		    // The problem is now conflicting/satisfied, we're
		    // done.
		    return res;
		}
	    }
	}

	// Keep searching for the unassigned variable 
	arr = c.neg;
	for( int j=0; j<arr.length; j++ ){
	    int v = arr[j];
	    if( assignment[v] == UNASSIGNED ){
		// We have found the unassigned one, propagate it.
		if( tracePropagation ){
		    System.err.println( "Propagating negative unit variable " + v + " from clause " + c );
		}
		int res = propagateNegAssignment( p, v );
		if( (res != 0) || !doVerification ){
		    // The problem is now conflicting/satisfied, we're
		    // done.
		    return res;
		}
	    }
	}
	return SATProblem.UNDETERMINED;
    }

    /**
     * Registers the fact that the specified clause is satisfied.
     * @param p The SAT problem.
     * @param cno The index of the clause that is now satisifed.
     * @return CONFLICTING if the problem is now in conflict, SATISFIED if the problem is now satisified, or UNDETERMINED otherwise
     */
    private int markClauseSatisfied( SATProblem p, int cno )
    {
	boolean hasPure = false;

	satisfied[cno] = true;
	unsatisfied--;
	if( unsatisfied == 0 ){
	    return SATProblem.SATISFIED;
	}
	Clause c = p.clauses[cno];
	if( tracePropagation ){
	    System.err.println( "Clause " + c + " is now satisfied, " + unsatisfied + " to go" );
	}

	int pos[] = c.pos;
	for( int i=0; i<pos.length; i++ ){
	    int var = pos[i];

	    int pc = --posclauses[var];
	    if( doVerification ){
	        verifyClauseCount( p, var );
	    }
	    if( propagatePureVariables && pc == 0 ){
		if( assignment[var] == UNASSIGNED ){
                    if( negclauses[var] != 0 ){
                        if( tracePropagation ){
                            System.err.println( "Variable " + var + " only occurs negatively (0," + negclauses[var] + ")"  );
                        }
                        // Only register the fact that there is an pure
                        // variable. Don't propagate it yet, since the
                        // adminstration is inconsistent at the moment.
                        hasPure = true;
                    }
		}
	    }
	}
	int neg[] = c.neg;
	for( int i=0; i<neg.length; i++ ){
	    int var = neg[i];

	    int nc = --negclauses[var];
	    if( doVerification ){
	        verifyClauseCount( p, var );
	    }
	    if( propagatePureVariables && nc == 0 ){
		if( assignment[var] == UNASSIGNED ){
                    if( posclauses[var] != 0 ){
                        if( tracePropagation ){
                            System.err.println( "Variable " + var + " only occurs positively (" + posclauses[var] + ",0)"  );
                        }
                        // Only register the fact that there is an pure
                        // variable. Don't propagate it yet, since the
                        // adminstration is inconsistent at the moment.
                        hasPure = true;
                    }
		}
	    }
	}
	if( propagatePureVariables && hasPure ){
	    // Now propagate the pure variables.
	    for( int i=0; i<pos.length; i++ ){
		int var = pos[i];

		if( assignment[var] == UNASSIGNED && posclauses[var] == 0 && negclauses[var] != 0 ){
		    int res = propagateNegAssignment( p, var );
		    if( res != 0 ){
			return res;
		    }
		}
	    }
	    for( int i=0; i<neg.length; i++ ){
		int var = neg[i];

		if( assignment[var] == UNASSIGNED && negclauses[var] == 0 && posclauses[var] != 0 ){
		    int res = propagatePosAssignment( p, var );
		    if( res != 0 ){
			return res;
		    }
		}
	    }
	}
	return SATProblem.UNDETERMINED;
    }

    private void dumpAssignments()
    {
        Helpers.dumpAssignments( "Assignments", assignment );
    }

    /**
     * Propagates the fact that variable 'var' is true.
     * @return CONFLICTING if the problem is now in conflict, SATISFIED if the problem is now satisified, or UNDETERMINED otherwise
     */
    public int propagatePosAssignment( SATProblem p, int var )
    {
        assignment[var] = 1;
	boolean hasUnitClauses = false;

	if( tracePropagation ){
	    System.err.println( "Propagating assignment v" + var + "=1" );
	}
	// Deduct this clause from all clauses that contain this as a
	// negative term.
	int neg[] = p.getNegClauses( var );
	for( int i=0; i<neg.length; i++ ){
	    int cno = neg[i];

            // Deduct the old info of this clause.
            posinfo[var] -= p.reviewer.info( terms[cno] );
	    terms[cno]--;
	    if( terms[cno] == 0 ){
                analyzeConflict( p, cno, var );
	        return SATProblem.CONFLICTING;
	    }
	    if( terms[cno] == 1 ){
		// Remember that we saw a unit clause, but don't
		// propagate it yet, since the administration is inconsistent.
		hasUnitClauses = true;
	    }
            else {
                // Add the new information of this clause.
                posinfo[var] += p.reviewer.info( terms[cno] );
            }
	}

	// Mark all clauses that contain this variable as a positive
	// term as satisfied.
	int pos[] = p.getPosClauses( var );
	for( int i=0; i<pos.length; i++ ){
	    int cno = pos[i];

	    if( !satisfied[cno] ){
                posinfo[var] -= p.reviewer.info( terms[cno] );
		int res = markClauseSatisfied( p, cno );

		if( res != 0 ){
		    return res;
		}
	    }
	}

	// Now propagate unit clauses if there are any.
	if( hasUnitClauses ){
	    for( int i=0; i<neg.length; i++ ){
		int cno = neg[i];

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
	return SATProblem.UNDETERMINED;
    }

    /**
     * Propagates the fact that variable 'var' is false.
     * @return CONFLICTING if the problem is now in conflict, SATISFIED if the problem is now satisified, or UNDETERMINED otherwise
     */
    public int propagateNegAssignment( SATProblem p, int var )
    {
        assignment[var] = 0;
	boolean hasUnitClauses = false;

	if( tracePropagation ){
	    System.err.println( "Propagating assignment v" + var + "=0" );
	}
	// Deduct this clause from all clauses that contain this as a
	// Positive term.
	int pos[] = p.getPosClauses( var );
	for( int i=0; i<pos.length; i++ ){
	    int cno = pos[i];

            // Deduct the old info of this clause.
            posinfo[var] -= p.reviewer.info( terms[cno] );
	    terms[cno]--;
	    if( terms[cno] == 0 ){
                analyzeConflict( p, cno, var );
	        return SATProblem.CONFLICTING;
	    }
	    if( terms[cno] == 1 ){
		// Remember that we saw a unit clause, but don't
		// propagate it yet, since the administration is inconsistent.
		hasUnitClauses = true;
	    }
            else {
                // Add the new information of this clause.
                posinfo[var] += p.reviewer.info( terms[cno] );
            }
	}

	// Mark all clauses that contain this variable as a negative
	// term as satisfied.
	int neg[] = p.getNegClauses( var );
	for( int i=0; i<neg.length; i++ ){
	    int cno = neg[i];

	    if( !satisfied[cno] ){
                neginfo[var] -= p.reviewer.info( terms[cno] );
		int res = markClauseSatisfied( p, cno );

		if( res != 0 ){
		    return res;
		}
	    }
	}

	// Now propagate unit clauses if there are any.
	if( hasUnitClauses ){
	    for( int i=0; i<pos.length; i++ ){
		int cno = pos[i];

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
	return SATProblem.UNDETERMINED;
    }

    /**
     * Returns the best decision variable to branch on, or -1 if there is none.
     */
    public int getDecisionVariable()
    {
        int bestvar = -1;
        float bestinfo = -1;
        int bestmaxcount = 0;

        for( int i=0; i<assignment.length; i++ ){
            if( assignment[i] != UNASSIGNED ){
                // Already assigned, so not interesting.
                continue;
            }
            if( doVerification ){
                if( posinfo[i]<-0.01 || neginfo[i]<-0.01 ){
                    System.err.println( "Weird info for variable " + i + ": posinfo=" + posinfo[i] + ", neginfo=" + neginfo[i] );
                }
            }
            float info = Math.max( posinfo[i], neginfo[i] );
            if( info>=bestinfo ){
                int maxcount = Math.max( posclauses[i], negclauses[i] );

                if( (info>bestinfo) || (maxcount<bestmaxcount) ){
                    // This is a better one.
                    bestvar = i;
                    bestinfo = info;
                    bestmaxcount = maxcount;
                }
            }
        }
        return bestvar;
    }

    /**
     * Returns true iff the given variable has more information as
     * positive variable than as negative variable.
     * @param var the variable
     */
    public boolean posDominant( int var )
    {
        return (posinfo[var]>neginfo[var]);
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
     * clauses and pure variables that we can find.
     * @param p The SAT problem this is the context for.
     * @return CONFLICTING if the problem is now in conflict, SATISFIED if the problem is now satisified, or UNDETERMINED otherwise
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
	// Search for and propagate pure variables.
	for( int i=0; i<assignment.length; i++ ){
	    if( assignment[i] != UNASSIGNED || (posclauses[i] == 0 && negclauses[i] == 0) ){
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
	return SATProblem.UNDETERMINED;
    }
}
