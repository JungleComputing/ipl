// $Id$

/**
 * The context of the solve method. The fields in this class are cloned
 * for every recursion of the solve() method.
 */
public final class SATContext implements java.io.Serializable {
    /**
     * A symbolic name for the `unassigned' value in
     * the `assignment' array.
     */
    private static final int UNASSIGNED = -1;

    /** The number of open terms of each clause. */
    private int terms[];

    /** The assignments to all variables. */
    int assignment[];

    /**
     * The antecedent clause of each variable, or -1 if it is a decision
     * variable, or isn't assigned yet.
     */
    int antecedent[];

    /**
     * The decision level of each variable, or -1 if 
     * it isn't assigned yet.
     */
    int dl[];

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
	int dl[],
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
        this.dl = dl;
	satisfied = sat;
	unsatisfied = us;
	posclauses = poscl;
	negclauses = negcl;
        this.posinfo = posinfo;
        this.neginfo = neginfo;
    }

    private static final boolean tracePropagation = false;
    private static final boolean traceLearning = false;
    private static final boolean traceResolutionChain = false;

    private static final boolean doVerification = false;
    private static final boolean doLearning = true;
    private static final boolean propagatePureVariables = true;

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
        int dl[] = new int[assignment.length];

        for( int i=0; i<antecedent.length; i++ ){
            antecedent[i] = -1;
            dl[i] = -1;
        }
        return new SATContext(
            p.buildTermCounts(),
            assignment,
            antecedent,
            dl,
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
	    (int []) dl.clone(),
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

    private void dumpAntecedents( String indent, SATProblem p, int cno )
    {
        Clause c = p.clauses[cno];
        System.err.println( indent + c );
	int arr[] = c.pos;
        String indent1 = indent + " ";
        
        for( int i=0; i<arr.length; i++ ){
            int v = arr[i];
            int a = antecedent[v];

            if( a>=0 ){
                if( a == cno ){
                    System.err.println( indent1 + "implication: v" + v + "=" + assignment[v] + "@" + dl[v] );
                }
                else {
                    dumpAntecedents( indent1, p, a );
                }
            }
            else {
                System.err.println( indent1 + "decision variable v" + v + "=" + assignment[v] + "@" + dl[v] );
            }
        }
        arr = c.neg;
        for( int i=0; i<arr.length; i++ ){
            int v = arr[i];
            int a = antecedent[v];

            if( a>=0 ){
                if( a == cno ){
                    System.err.println( indent1 + "implication: v" + v + "=" + assignment[v] + "@" + dl[v] );
                }
                else {
                    dumpAntecedents( indent1, p, a );
                }
            }
            else {
                System.err.println( indent1 + "decision variable v" + v + "=" + assignment[v] + "@" + dl[v] );
            }
        }
    }

    /**
     * Given a clause, returns the implication of this clause.
     * @param c The clause to examine.
     * @return The variable that is implied by this clause, or -1 if there is none (i.e. the clause is in conflict), or -2 if there is morethan one.
     */
    private int getImplication( Clause c, int cno )
    {
	int arr[] = c.pos;
        int res = -1;
        
        for( int i=0; i<arr.length; i++ ){
            int v = arr[i];
            int a = antecedent[v];

            if( a == cno ){
                if( res != -1 ){
                    // There already is an implication. This isn't right.
                    return -2;
                }
                res = v;
            }
        }
        arr = c.neg;
        for( int i=0; i<arr.length; i++ ){
            int v = arr[i];
            int a = antecedent[v];

            if( a == cno ){
                if( res != -1 ){
                    // There already is an implication. This isn't right.
                    return -2;
                }
                res = v;
            }
        }
        return res;
    }

    /**
     * Given a clause index, print the implications that cause this
     * clause to be satisfied/conflicting.
     * @param indent The indent string to use for the output.
     * @param p The sat problem.
     * @param cno The clause to dump.
     */
    private void dumpImplications( String indent, SATProblem p, int cno )
    {
        Clause c = p.clauses[cno];
        int impl = getImplication( c, cno );

        String conclusion;
        if( impl == -2 ){
            conclusion = "(nothing to conclude)";
        }
        else if( impl == -1 ){
            conclusion = "(conflict)";
        }
        else {
            conclusion = "==> v" + impl + "=" + assignment[impl];
        }
        System.err.println( indent + c + " " + conclusion );
	int arr[] = c.pos;
        String indent1 = indent + " ";
        
        for( int i=0; i<arr.length; i++ ){
            int v = arr[i];
            int a = antecedent[v];

            if( a>=0 ){
                if( a != cno ){
                    dumpImplications( indent1, p, a );
                }
            }
        }
        arr = c.neg;
        for( int i=0; i<arr.length; i++ ){
            int v = arr[i];
            int a = antecedent[v];

            if( a>=0 ){
                if( a != cno ){
                    dumpImplications( indent1, p, a );
                }
            }
        }
    }

    private int calculateNearestDominator( SATProblem p, int arr[], int cno, int level, int dist[], int distFromConflict )
    {
        int bestDom = -1;
        int bestDist = satisfied.length;
        int bestVar = -1;

        for( int i=0; i<arr.length; i++ ){
            int v = arr[i];
            int a = antecedent[v];

            if( dl[v] == level ){
                // The variable was deduced at our level, so it's
                // interesting.

                if( a<0 ){
                    // The decision variable at our level, not interesting.
                }
                else {
                    // The variable is not a decision variable.
                    if( a == cno ){
                        // The implication variable of this clause,
                        // not interesting.
                    }
                    else {
                        // Variable is not implied by this clause, we're
                        // still interested.
                        int newDom = calculateNearestDominator( p, a, level, dist, distFromConflict+1 );
                        if( newDom != -1 ){
                            if( bestDom == -1 || (dist[newDom]<bestDist) ){
                                bestDom = newDom;
                                bestDist = dist[newDom];
                                bestVar = v;
                            }
                        }
                    }
                }
            }
        }
        return bestDom;
    }

    /**
     * Returns the index of the nearest dominator clause, or -1 if there
     * is no dominator.
     */
    private int calculateNearestDominator( SATProblem p, int cno, int level, int dist[], int distFromConflict )
    {
        Clause c = p.clauses[cno];
        if( traceResolutionChain ){
            System.err.println( "Calculating nearest dominator of clause " + c );
        }
        if( dist[cno] != 0 ){
            // We've been here before, so this is a dominator.
            if( dist[cno]>distFromConflict ){
                dist[cno] = distFromConflict;
            }
            if( traceResolutionChain ){
                System.err.println( "We cross an earlier path with clause " + c + " in it: dominator" );
            }
            return cno;
        }
        dist[cno] = distFromConflict;
        int bestDom;

        int bestPosDom = calculateNearestDominator( p, c.pos, cno, level, dist, distFromConflict );
        int bestNegDom = calculateNearestDominator( p, c.neg, cno, level, dist, distFromConflict );
        if( bestPosDom == -1 ){
            bestDom = bestNegDom;
        }
        else {
           if( bestNegDom == -1 ){
               bestDom = bestPosDom;
           }
           else {
               if( dist[bestNegDom]<dist[bestPosDom] ){
                   bestDom = bestNegDom;
               }
               else {
                   bestDom = bestPosDom;
               }
           }
        }
        if( traceResolutionChain ){
            System.err.println( "Nearest dominator of clause " + c + " is " + bestDom );
        }
        return bestDom;
    }

    int calculateNearestDominator( SATProblem p, int cno, int level )
    {
        // The distance of each clause to the conflicting clause. The 
        // conflicting clause itself gets distance 1, so that we can use
        // the default value 0  as indication that we haven't considered
	// this clause yet.
        int dist[] = new int[satisfied.length];
        return calculateNearestDominator( p, cno, level, dist, 1 );
    }

    /**
     * Given the index of a conflicting clause, builds a conflict clause.
     * Returns null if no helpful clause can be constructed.
     * @param p The SAT problem.
     * @param cno The clause that is in conflict.
     * @param var The variable that is in conflict.
     * @param level The decision level of the conflict.
     * @return A new clause that should improve the efficiency of the search process, or null if no helpful clause can be constructed.
     */
    private Clause buildConflictClause( SATProblem p, int cno, int var, int level )
    {
        boolean changed = false;
        boolean anyChange = false;
        Clause res = p.clauses[cno];

        int bestDom = calculateNearestDominator( p, cno, level );
        do {
            changed = false;
            int arr[] = res.pos;
            for( int i=0; i<arr.length; i++ ){
                int v = arr[i];

                if( dl[v] == level ){
                    int a = antecedent[v];

                    if( a>=0 && a != bestDom ){
                        Clause newres = Clause.resolve( res, p.clauses[a], v );
                        if( traceLearning ){
                            System.err.println( "Resolving on v" + v + ":" );
                            System.err.println( "  " + res );
                            System.err.println( "  " + p.clauses[a] + " =>" );
                            System.err.println( "  " + newres );
                        }
                        changed = true;
                        anyChange = true;
                        res = newres;
                        break;
                    }
                }
            }
            arr = res.neg;
            for( int i=0; i<arr.length; i++ ){
                int v = arr[i];

                if( dl[v] == level ){
                    int a = antecedent[v];

                    if( a>=0 && a != bestDom ){
                        Clause newres = Clause.resolve( res, p.clauses[a], v );
                        if( traceLearning ){
                            System.err.println( "Resolving on v" + v + ":" );
                            System.err.println( "  " + res );
                            System.err.println( "  " + p.clauses[a] + " =>" );
                            System.err.println( "  " + newres );
                        }
                        changed = true;
                        anyChange = true;
                        res = newres;
                        break;
                    }
                }
            }
        } while( changed );
        if( !anyChange ){
            return null;
        }
        return res;
    }

    /**
     * Calculate the restart level.
     */
    private int calculateRestartLevel( Clause c, int mylevel )
    {
	int level = -1;

	int arr[] = c.pos;
        
        for( int i=0; i<arr.length; i++ ){
            int v = arr[i];

	    if( dl[v] != mylevel && dl[v]>level  ){
		level = dl[v];
	    }
        }
        arr = c.neg;
        for( int i=0; i<arr.length; i++ ){
            int v = arr[i];

	    if( dl[v] != mylevel && dl[v]>level ){
		level = dl[v];
	    }
        }
	return level;
    }

    /**
     * Registers the fact that the specified clause is in conflict with
     * the current assignments. This method throws a
     * restart exception if that is useful.
     * @param p The SAT problem.
     * @param cno The clause that is in conflict.
     */
    private void analyzeConflict( SATProblem p, int cno, int var, int level )
        throws SATRestartException
    {
        if( tracePropagation | traceLearning | traceResolutionChain ){
            System.err.println( "Clause " + p.clauses[cno] + " conflicts with v" + var + "=" + assignment[var] );
            dumpAssignments();
            if( traceResolutionChain ){
                dumpImplications( "", p, cno );
            }
        }
	if( doLearning ){
	    Clause cc = buildConflictClause( p, cno, var, level );
	    if( cc == null ){
		if( traceLearning ){
		    System.err.println( "No interesting conflict clause could be constructed" );
		}
	    }
	    else {
		if( traceLearning ){
		    System.err.println( "Added conflict clause " + cc );
		}
		p.addConflictClause( cc );
		int rl = calculateRestartLevel( cc, level );
		if( traceLearning ){
		    System.err.println( "Restarting at level " + rl + " (now at " + level + ")" );
		}
		if( rl<(level-1) ){
		    throw new SATRestartException( rl );
		}
	    }
	}
    }

    /**
     * Propagates the specified unit clause.
     * @param p the SAT problem to solve
     * @param i the index of the unit clause
     * @return CONFLICTING if the problem is now in conflict, SATISFIED if the problem is now satisified, or UNDETERMINED otherwise
     */
    private int propagateUnitClause( SATProblem p, int i, int level )
        throws SATRestartException
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
                antecedent[v] = i;
		int res = propagatePosAssignment( p, v, level );
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
                antecedent[v] = i;
		int res = propagateNegAssignment( p, v, level );
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
     * Update the adminstration for any new clauses in the specified
     * SAT problem.
     * @param p The SAT problem.
     */
    public void update( SATProblem p )
    {
        int newCount = p.getClauseCount();

        if( newCount>terms.length ){
            int oldCount = terms.length;
            // New clauses have been added. Enlarge the arrays related
            // to the clauses, and fill them with the correct values.

            int newterms[] = new int[newCount];
            System.arraycopy( terms, 0, newterms, 0, terms.length );
            terms = newterms;

            boolean newsatisfied[] = new boolean[newCount];
            System.arraycopy( satisfied, 0, newsatisfied, 0, satisfied.length );
            satisfied = newsatisfied;

            for( int i=oldCount; i<newCount; i++ ){
                Clause cl = p.clauses[i];

                int nterm = cl.getTermCount( assignment );
		float info = p.reviewer.info( nterm );
                terms[i] = nterm;
                boolean issat = cl.isSatisfied( assignment );
                if( !issat ){
                    unsatisfied++;
                    cl.registerInfo( posinfo, neginfo, info );
                }
                cl.registerVariableCounts( posclauses, negclauses );
                if( doVerification ){
                    verifyTermCount( p, i );
                }
            }
        }
    }

    /**
     * Registers the fact that the specified clause is satisfied.
     * @param p The SAT problem.
     * @param cno The index of the clause that is now satisifed.
     * @return CONFLICTING if the problem is now in conflict, SATISFIED if the problem is now satisified, or UNDETERMINED otherwise
     */
    private int markClauseSatisfied( SATProblem p, int cno, int level )
        throws SATRestartException
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
                    antecedent[var] = cno;
		    int res = propagateNegAssignment( p, var, level );
		    if( res != 0 ){
			return res;
		    }
		}
	    }
	    for( int i=0; i<neg.length; i++ ){
		int var = neg[i];

		if( assignment[var] == UNASSIGNED && negclauses[var] == 0 && posclauses[var] != 0 ){
                    antecedent[var] = cno;
		    int res = propagatePosAssignment( p, var, level );
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
    public int propagatePosAssignment( SATProblem p, int var, int level )
        throws SATRestartException
    {
        assignment[var] = 1;
        dl[var] = level;
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
            posinfo[var] -= p.reviewer.info( terms[cno] );
	    terms[cno]--;
	    if( terms[cno] == 0 ){
                analyzeConflict( p, cno, var, level );
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
	IntVector pos = p.getPosClauses( var );
	sz = pos.size();
	for( int i=0; i<sz; i++ ){
	    int cno = pos.get( i );

	    if( !satisfied[cno] ){
                posinfo[var] -= p.reviewer.info( terms[cno] );
		int res = markClauseSatisfied( p, cno, level );

		if( res != 0 ){
		    return res;
		}
	    }
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
		    int res = propagateUnitClause( p, cno, level );
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
    public int propagateNegAssignment( SATProblem p, int var, int level )
        throws SATRestartException
    {
        assignment[var] = 0;
        dl[var] = level;
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
            posinfo[var] -= p.reviewer.info( terms[cno] );
	    terms[cno]--;
	    if( terms[cno] == 0 ){
                analyzeConflict( p, cno, var, level );
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
	IntVector neg = p.getNegClauses( var );
	sz = neg.size();
	for( int i=0; i<sz; i++ ){
	    int cno = neg.get( i );

	    if( !satisfied[cno] ){
                neginfo[var] -= p.reviewer.info( terms[cno] );
		int res = markClauseSatisfied( p, cno, level );

		if( res != 0 ){
		    return res;
		}
	    }
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
		    int res = propagateUnitClause( p, cno, level );
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
	//System.err.println( "Best var: v" + bestvar + " has info " + bestinfo );
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
        throws SATRestartException
    {
	// Search for and propagate unit clauses.
	for( int i=0; i<terms.length; i++ ){
	    if( terms[i] == 1 ){
		int res = propagateUnitClause( p, i, 0 );
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
		int res = propagateNegAssignment( p, i, 0 );
		if( res != 0 ){
		    return res;
		}
	    }
	    else if( negclauses[i] == 0 ){
		int res = propagatePosAssignment( p, i, 0 );
		if( res != 0 ){
		    return res;
		}
	    }
	}
	return SATProblem.UNDETERMINED;
    }
}
