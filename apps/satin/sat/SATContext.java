// $Id$

/**
 * The context of the solve method. The fields in this class are cloned
 * for every recursion of the solve() method.
 */
public class SATContext implements java.io.Serializable {
    /**
     * A symbolic name for the `unassigned' value in
     * the `assignment' array.
     */
    private static final int UNASSIGNED = -1;

    /** The number of open terms of each clause. */
    private int terms[];

    /** The assignments to all variables. */
    int assignment[];

    /** The antecedent of each variable, or -1 if it is a decision variable. */
    int antecedent[];

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
    private SATContext(
	int tl[],
	int al[],
	int atc[],
	int poscl[],
	int negcl[],
	float posinfo[],
	float neginfo[],
	boolean sat[],
	int us
    ){
	terms = tl;
	assignment = al;
	antecedent = atc;
	satisfied = sat;
	unsatisfied = us;
	posclauses = poscl;
	negclauses = negcl;
        this.posinfo = posinfo;
        this.neginfo = neginfo;
    }

    private static final boolean tracePropagation = false;
    private static final boolean traceLearning = true;
    private static final boolean doVerification = false;

    /**
     * Constructs a SAT context based on the given SAT problem.
     * @param p The SAT problem to create the context for.
     * @return the constructed context
     */
    public static SATContext buildSATContext( SATProblem p )
    {
        int cno = p.getClauseCount();
        int assignment[] = p.buildInitialAssignments();
        int antecedent[] = new int[assignment.length];

        for( int i=0; i<antecedent.length; i++ ){
            antecedent[i] = -1;
        }
        return new SATContext(
            p.buildTermCounts(),
            assignment,
            antecedent,
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
        return new SATContext(
	    (int []) terms.clone(),
	    (int []) assignment.clone(),
	    (int []) antecedent.clone(),
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
	    System.err.println( "Error: clause count of v" + var + " says (" + posclauses[var] + "," + negclauses[var] + "), not (" + poscount + "," + negcount + ")" );
	    posclauses[var] = poscount;
	    negclauses[var] = negcount;
	}
    }

    /**
     * Registers the fact that the specified clause is in conflict with
     * the current assignments. If useful, this method may throw a
     * restart exception.
     * @param p The SAT problem.
     * @param cno The clause that is in conflict.
     */
    private void analyzeConflict( SATProblem p, int cno, int var )
    {
        if( tracePropagation | traceLearning ){
            System.err.println( "Clause " + p.clauses[cno] + " conflicts with v" + var + "=" + assignment[var] );
            dumpAssignments();
        }
        if( traceLearning ){
            Clause c = p.clauses[cno];
            int arr[] = c.pos;

            for( int i=0; i<arr.length; i++ ){
                int v = arr[i];

                String chain = "v" + v + "=" + assignment[v];
                while( antecedent[v] != -1 ){
                    int a = antecedent[v];
                    chain = "v" + a + "=" + assignment[a] + " -> " + chain;
                    v = a;
                }
                System.err.println( "@ " + chain );
            }
            arr = c.neg;
            for( int i=0; i<arr.length; i++ ){
                int v = arr[i];

                String chain = "v" + v + "=" + assignment[v];
                while( antecedent[v] != -1 ){
                    int a = antecedent[v];
                    chain = "v" + a + "=" + assignment[a] + " -> " + chain;
                    v = a;
                }
                System.err.println( "@ " + chain );
            }
        }
    }

    /**
     * Propagates the specified unit clause.
     * @param p the SAT problem to solve
     * @param i the index of the unit clause
     * @return CONFLICTING if the problem is now in conflict, SATISFIED if the problem is now satisified, or UNDETERMINED otherwise
     */
    private int propagateUnitClause( SATProblem p, int i, int antec )
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
	boolean foundIt = false;
	if( tracePropagation ){
	    System.err.println( "Propagating unit clause " + c );
	}
	// Now search for the variable that isn't satisfied.
	for( int j=0; j<arr.length; j++ ){
	    int v = arr[j];

	    if( assignment[v] == UNASSIGNED ){
		if( foundIt ){
		    System.err.println( "Error: a unit clause with multiple unassigned variables" );
		    return SATProblem.UNDETERMINED;
		}
		// We have found the unassigned one, propagate it.
		if( tracePropagation ){
		    System.err.println( "Propagating positive unit variable " + v + " from clause " + c );
		}
                antecedent[v] = antec;
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
	    if( assignment[v] == UNASSIGNED ){
		if( foundIt ){
		    System.err.println( "Error: a unit clause with multiple unassigned variables" );
		    return SATProblem.UNDETERMINED;
		}
		// We have found the unassigned one, propagate it.
		if( tracePropagation ){
		    System.err.println( "Propagating negative unit variable " + v + " from clause " + c );
		}
                antecedent[v] = antec;
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
	return SATProblem.UNDETERMINED;
    }

    /**
     * Registers the fact that the specified clause is satisfied.
     * @return CONFLICTING if the problem is now in conflict, SATISFIED if the problem is now satisified, or UNDETERMINED otherwise
     */
    private int markClauseSatisfied( SATProblem p, int cno, int antec )
    {
	boolean hasPure = false;

	satisfied[cno] = true;
	unsatisfied--;
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
	    if( pc == 0 ){
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
	    if( nc == 0 ){
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
	if( hasPure ){
	    // Now propagate the pure variables.
	    for( int i=0; i<pos.length; i++ ){
		int var = pos[i];

		if( assignment[var] == UNASSIGNED && posclauses[var] == 0 && negclauses[var] != 0 ){
                    antecedent[var] = antec;
		    int res = propagateNegAssignment( p, var );
		    if( res != 0 ){
			return res;
		    }
		}
	    }
	    for( int i=0; i<neg.length; i++ ){
		int var = neg[i];

		if( assignment[var] == UNASSIGNED && negclauses[var] == 0 && posclauses[var] != 0 ){
                    antecedent[var] = antec;
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
        Helpers.dumpAssignments( "Assignments", assignment, antecedent );
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
	    System.err.println( "Propagating assignment v" + var + "=true" );
	}
	// Deduct this clause from all clauses that contain this as a
	// negative term.
	IntVector neg = p.getNegClauses( var );
	int sz = neg.size();
	for( int i=0; i<sz; i++ ){
	    int cno = neg.get( i );

            // Deduct the old info of this clause.
            posinfo[var] -= Helpers.information( terms[cno] );
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
                posinfo[var] += Helpers.information( terms[cno] );
            }
	}

	// Mark all clauses that contain this variable as a positive
	// term as satisfied.
	IntVector pos = p.getPosClauses( var );
	sz = pos.size();
	for( int i=0; i<sz; i++ ){
	    int cno = pos.get( i );

	    if( !satisfied[cno] ){
                posinfo[var] -= Helpers.information( terms[cno] );
		int res = markClauseSatisfied( p, cno, var );

		if( res != 0 ){
		    return res;
		}
	    }
	}
	if( unsatisfied == 0 ){
	    // All clauses are now satisfied, we have a winner!
	    return SATProblem.SATISFIED;
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
		    int res = propagateUnitClause( p, cno, var );
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
	    System.err.println( "Propagating assignment v" + var + "=false" );
	}
	// Deduct this clause from all clauses that contain this as a
	// Positive term.
	IntVector pos = p.getPosClauses( var );
	int sz = pos.size();
	for( int i=0; i<sz; i++ ){
	    int cno = pos.get( i );

            // Deduct the old info of this clause.
            posinfo[var] -= Helpers.information( terms[cno] );
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
                posinfo[var] += Helpers.information( terms[cno] );
            }
	}

	// Mark all clauses that contain this variable as a negative
	// term as satisfied.
	IntVector neg = p.getNegClauses( var );
	sz = neg.size();
	for( int i=0; i<sz; i++ ){
	    int cno = neg.get( i );

	    if( !satisfied[cno] ){
                neginfo[var] -= Helpers.information( terms[cno] );
		int res = markClauseSatisfied( p, cno, var );

		if( res != 0 ){
		    return res;
		}
	    }
	}
	if( unsatisfied == 0 ){
	    // All clauses are now satisfied, we have a winner!
	    return SATProblem.SATISFIED;
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
		    int res = propagateUnitClause( p, cno, var );
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
        if( false ){
            // For the moment we return the variable that is used the most.
            int bestvar = -1;
            int bestusecount = 0;
            int bestmaxcount = 0;

            for( int i=0; i<assignment.length; i++ ){
                if( assignment[i] != UNASSIGNED ){
                    // Already assigned, so not interesting.
                    continue;
                }
                int usecount = posclauses[i] + negclauses[i];
                if( usecount>=bestusecount ){
                    // Use maxcount to decide when usecounts are equal.
                    int maxcount = Math.max( posclauses[i], negclauses[i] );

                    if( (usecount>bestusecount) || (maxcount>bestmaxcount) ){
                        // This is a better one.
                        bestvar = i;
                        bestusecount = usecount;
                        bestmaxcount = maxcount;

                    }
                }
            }
            return bestvar;
        }
        else {
            int bestvar = -1;
            float bestinfo = -1;
            int bestmaxcount = 0;

            for( int i=0; i<assignment.length; i++ ){
                if( assignment[i] != UNASSIGNED ){
                    // Already assigned, so not interesting.
                    continue;
                }
                if( doVerification ){
                    if( posinfo[i]<0.01 || neginfo[i]<0.01 ){
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
            //System.err.println( "Variable " + bestvar + " has " + bestinfo + " bits information" );
            return bestvar;
        }
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
		int res = propagateUnitClause( p, i, -1 );
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
