// File: $Id$
//
// A SAT solver. Given a symbolic boolean equation in CNF, find all
// sets of assignments that make this equation true.

import java.io.File;


public class SATSolver {
    static class Context {
	SATProblem problem;	// The problem to solve.
	SATSolution solutions[];
	int solutioncount;
	int assignments[];
	boolean satisfied[];
	int terms[];
	int level;
	int assumptions;
    }

    static final boolean traceSolver = false;
    static final boolean printSatSolutions = true;
    static int label = 0;

    // Given a set of clauses and a variable `var' that we know is true,
    // propagate this variable to the clauses. 
    static void propagateTrueValue( Context ctx, final int var )
    {
	final IntVector poscls = ctx.problem.getPosClauses( var );
	final int sz = poscls.size();
	boolean satisfied[] = ctx.satisfied;

	for( int ix=0; ix<sz; ix++ ){
	    int cno = poscls.get( ix );

	    satisfied[cno] = true;
	    if( traceSolver ){
		System.out.println( "Assignment var[" + var + "]=true satisfies clause " + cno );
	    }
	}
	int terms[] = ctx.terms;
	final IntVector neg = ctx.problem.getNegClauses( var );
	final int szt = neg.size();

	for( int ix=0; ix<szt; ix++ ){
	    int cno = neg.get( ix );

	    terms[cno]--;
	}
    }

    // Given a set of clauses and a variable `var' that we know is false,
    // propagate this variable to the clauses. 
    static void propagateFalseValue( Context ctx, final int var )
    {
	final IntVector neg = ctx.problem.getNegClauses( var );
	final int sz = neg.size();
	boolean satisfied[] = ctx.satisfied;

	for( int ix=0; ix<sz; ix++ ){
	    int cno = neg.get( ix );

	    satisfied[cno] = true;
	    if( traceSolver ){
		System.out.println( "Assignment var[" + var + "]=false satisfies clause " + cno );
	    }
	}
	int terms[] = ctx.terms;
	final IntVector pos = ctx.problem.getPosClauses( var );
	final int szt = pos.size();

	for( int ix=0; ix<szt; ix++ ){
	    int cno = pos.get( ix );

	    terms[cno]--;
	}
    }

    // Given the current list of clause sizes, the length of that list,
    // the current clauses, and an the variables
    static int []buildNewTerms(
	final int old_terms[],
	final int old_satsize,
	final Clause clauses[],
	final int clauseCount,
	final int assignments[]
    )
    {
	int new_terms[] = new int[clauseCount];

        // Copy the old values as much as possible.
	System.arraycopy( old_terms, 0, new_terms, 0, old_terms.length );
	for( int it=old_terms.length; it<clauseCount; it++ ){
	    final Clause cl = clauses[it];

	    // Take the variable we are trying to assign to into account.
	    int n = 1;
	    final int pos[] = cl.pos;

	    for( int ix=0; ix<pos.length; ix++ ){
		if( assignments[pos[ix]] != 1 ){
		    n++;
		}
	    }
	    final int neg[] = cl.neg;

	    for( int ix=0; ix<neg.length; ix++ ){
		if( assignments[neg[ix]] != 0 ){
		    n++;
		}
	    }
	    new_terms[it] = n;
	}
	return new_terms;
    }

    // Given the current solver context, and a clause, verify that the clause
    // is in fact satisfied by the current assignments.
    static void verifyClauseSolution( Context ctx, final Clause cl )
    {
        int pos[] = cl.pos;
        int neg[] = cl.neg;
	int assignments[] = ctx.assignments;

	// We implement this by walking over all terms of the clause,
	// and comparing them with the assignments in ctx.assignments.
	// If one of them matches, the clause is satisfied, and we
	// can go away happy.
	for( int ix=0; ix<pos.length; ix++ ){
	    if( assignments[pos[ix]] == 1 ){
		// Yep, this one is satisfied.
	        return;
	    }
	}
	for( int ix=0; ix<neg.length; ix++ ){
	    if( assignments[neg[ix]] == 0 ){
		// Yep, this one is satisfied.
	        return;
	    }
	}
	System.err.println( "Verification failed: clause " + cl.label + " is not satisfied" );
    }

    // Given the current solver context, verify that it does in fact
    // represent a solution of the system.
    static void verifySolution( Context ctx )
    {
	// TODO: move this method to the SATProblem class.
        final Clause clauses[] = ctx.problem.clauses;

	for( int ix=0; ix<ctx.problem.getClauseCount(); ix++ ){
	    verifyClauseSolution( ctx, clauses[ix] );
	}
    }

    // Given a clauses and a list of assignments, return true iff the
    // assignments satisfy the clause.
    static boolean satisfiesClause( Clause cl, int al[] )
    {
	final int pos[] = cl.pos;

	// We implement this by walking over all terms of the clause,
	// and comparing them with the assignments in ctx.assignments.
	// If one of them matches, the clause is satisfied, and we
	// can return true.
	for( int ix=0; ix<pos.length; ix++ ){
	    int p = pos[ix];

	    if( al[p] == 1 ){
		return true;
	    }
	}
	final int neg[] = cl.neg;

	for( int ix=0; ix<neg.length; ix++ ){
	    int p = neg[ix];

	    if( al[p] == 0 ){
		return true;
	    }
	}
	return false;
    }

    // Given the clauses and a solution, return true iff the solution
    // satisfies the clauses.
    static boolean satisfiesSolution( Clause clauses[], int clauseCount, int  al[] )
    {
	for( int ix=0; ix<clauseCount; ix++ ){
	    if( !satisfiesClause( clauses[ix], al ) ){
		return false;
	    }
	}
	return true;
    }

    // Given an existing solution context, try to register a generalized
    // version of the solution.
    static void addGeneralizedSolutionList( Context ctx )
    {
	int gal[] = ctx.assignments;

	for( int ix=0; ix<gal.length; ix++ ){
	    if( gal[ix] != -1 ){
		int olda = gal[ix];

		gal[ix] = -1;
		if( satisfiesSolution( ctx.problem.clauses, ctx.problem.getClauseCount(), gal ) ){
		    if( traceSolver || printSatSolutions ){
			System.out.println( "Found a valid generalized solution by omitting term " + (olda==0?"!":"") + ix );
		    }
		    addSolutionList( ctx );
		}
		gal[ix] = olda;
	    }
	}
    }

    // Given an existing list of solutions, register the given assignment
    // list as a new solution.
    static void addSolutionList( Context ctx )
    {
	// There are no unsatisfied clauses left, so we have
	// found a solution.
	verifySolution( ctx );

	int oldcount = ctx.solutioncount;
	addGeneralizedSolutionList( ctx );
	if( ctx.solutioncount == oldcount ){
	    // There were no generalized solutions registered, so
	    // register this one.
	    SATSolution s = new SATSolution( ctx.assignments );

	    if( ctx.solutioncount>=ctx.solutions.length ){
		// TODO: grow the array if necessary.
		SATSolution sl[] = new SATSolution[ctx.solutioncount*2];
		System.arraycopy( ctx.solutions, 0, sl, 0, ctx.solutioncount );
		ctx.solutions = sl;
	    }
	    ctx.solutions[ctx.solutioncount++] = s;

	    if( printSatSolutions ){
		System.out.println( "Found a solution: " + s );
	    }

	    // We don't want this solution to turn up again. One way to do that
	    // is to add a clause that says this. This turns out to be easy:
	    // just add !(solution) to the clauses. Since a solution is the
	    // `and' of a number of terms, we can apply de-Morgan's rule to
	    // compute the inverse of a solution: it is the `or' of the
	    // inverse of all the terms in the solution. Given the way
	    // we represent solutions and clauses, we can just 
	    // use the list of positive variables of the solution as the list
	    // of negative variables of the new clause, and vice-versa.

	    if( traceSolver ){
		System.out.println( "Avoiding duplicate solutions with new clause" );
	    }
	    // Note that pos and neg are reversed.
	    ctx.problem.addClause( s.neg, s.pos );
	}
    }

    // Given a set of clauses, search for an unsatisfied clause, and try all
    // possible assignments that satisfy that clause. Return true iff
    // we have found a solution. If necessary recurse to fill more variable
    // assignments.
    static boolean tryAssignments( Context ctx )
    {
	boolean found_solution = false;

	// Remember the size of the 'satisified' vector at the start. We will
	// need it later, and the number of clauses may increase.
	int old_satsize = ctx.problem.getClauseCount();

	int min_terms = ctx.problem.getVariableCount();
	int ix = ctx.problem.getClauseCount();
	int terms[] = ctx.terms;

	// First search for the unsatisfied clause with the minimum number of
	// terms. Note that since clauses are added during the search
	// process, the `satisfied' array may be short. We know
	// that remaining clauses are not satisfied, since they represent
	// solutions that we already have registered.
	for( int is=0; is<ctx.problem.getClauseCount(); is++ ){
	    if( is>=ctx.satisfied.length || !ctx.satisfied[is] ){
		int thissz;

		if( is<terms.length ){
		    thissz = terms[is];
		}
		else {
		    Clause cl = ctx.problem.clauses[is];
		    thissz = cl.pos.length + cl.neg.length;
		}

		if( thissz<min_terms ){
		    ix = is;
		    min_terms = thissz;
		}
	    }
	}
	if( ix>=ctx.problem.getClauseCount() ){
	    // There are no unsolved clauses left, we have a solution.
	    addSolutionList( ctx );
	    return true;
	}

	// Clause with index `ix' is not satisfied. Try all possible
	// assignments to satisfy it.
	final Clause cl = ctx.problem.clauses[ix];

	// Start another branching level.
	ctx.level++;

	if( traceSolver ){
	    System.err.println( "(" + ctx.level + ") branching on " + min_terms + "-unsatisfied clause " + ix );
	}

	// We now have a clause `cl' that we try to satisfy by assigning
	// one of the variables in the clause to the correct value, and
	// seeing where that brings us.

	// First, try all positive variables.
	for( int i=0; i<cl.pos.length; i++ ){
	    int var = cl.pos[i];

	    if( ctx.assignments[var] == -1 ){
		// This assignment doesn't cause a conflict. Update the
		// administration, propagate unit clauses, and try
		// further assignments.

		ctx.assumptions++;
		ctx.assignments[var] = 1;
		if( traceSolver ){
		    System.err.println( "(" + ctx.level + ") trying assignment var[" + var + "]=true" );
		}
		{
		    // Create a new `satisfied' array for this recursion.
		    boolean old_satisfied[] = ctx.satisfied;
		    ctx.satisfied = new boolean[ctx.problem.getClauseCount()];
		    // We rely on the fact that a boolean array is initialized
		    // with false values, since we only explicitly fill
		    // the first part of the array.
		    System.arraycopy( old_satisfied, 0, ctx.satisfied, 0, old_satisfied.length );

		    // Create a new `terms' array for this recursion.
		    int old_terms[] = ctx.terms;
		    ctx.terms = buildNewTerms( old_terms, old_satisfied.length, ctx.problem.clauses, ctx.problem.getClauseCount(), ctx.assignments );

		    // Our clause is satisfied...
		    ctx.satisfied[ix] = true;
		    propagateTrueValue( ctx, var );
		    found_solution |= tryAssignments( ctx );
		    ctx.satisfied = old_satisfied;
		    ctx.terms = old_terms;
		}
		if( traceSolver ){
		    System.err.println( "(" + ctx.level + ") retracting assignment var[" + var + "]=true" );
		}

		// Retract this assignment
		ctx.assignments[var] = -1;
	    }
	}

	// Then, try all negative variables.
	for( int i=0; i<cl.neg.length; i++ ){
	    int var = cl.neg[i];

	    if( ctx.assignments[var] == -1 ){
		// This assignment doesn't cause a conflict. Update the
		// administration, propagate unit clauses, and try
		// further assignments.

		if( traceSolver ){
		    System.err.println( "(" + ctx.level + ") trying assignment var["  + var + "]=false" );
		}

		ctx.assumptions++;
		ctx.assignments[var] = 0;

		{
		    // Create a new `satisfied' array for this recursion.
		    boolean old_satisfied[] = ctx.satisfied;
		    ctx.satisfied = new boolean[ctx.problem.getClauseCount()];

		    // We rely on the fact that a boolean array is initialized
		    // with false values. 
		    System.arraycopy( old_satisfied, 0, ctx.satisfied, 0, old_satisfied.length );

		    // Create a new `terms' array for this recursion.
		    int old_terms[] = ctx.terms;
		    ctx.terms = buildNewTerms( old_terms, old_satsize, ctx.problem.clauses, ctx.problem.getClauseCount(), ctx.assignments );

		    // Our clause is satisfied...
		    ctx.satisfied[ix] = true;

		    propagateFalseValue( ctx, var );
		    found_solution |= tryAssignments( ctx );
		    ctx.satisfied = old_satisfied;
		    ctx.terms = old_terms;
		}

		if( traceSolver ){
		    System.err.println( "(" + ctx.level + ") retracting assignment var[" + var + "]=false" );
		}

		// Retract this assignment
		ctx.assignments[var] = -1;
	    }
	}

	// Close the branching level.
	ctx.level--;

	return found_solution;
    }


    // Given a list of symbolic clauses, produce a list of solutions.
    static SATSolution [] solveSystem( final SATProblem p )
    {
	Context ctx = new Context();
	ctx.problem = p;
	ctx.assignments = new int[p.getVariableCount()];
	// The list will be grown if necessary.
	ctx.solutions = new SATSolution[20];
	ctx.solutioncount = 0;
	ctx.level = 0;
	ctx.assumptions = 0;
	boolean ok;

	label = 0;
	ctx.problem.clauses = (Clause[]) p.clauses.clone();

	// Now start with a vector of unassigned variables.
	for( int ix=0; ix<ctx.problem.getVariableCount(); ix++ ){
	    ctx.assignments[ix] = -1;
	}
	ctx.satisfied = new boolean[ctx.problem.getClauseCount()];
	int terms[] = new int[ctx.problem.getClauseCount()];
	for( int ix=0; ix<ctx.problem.getClauseCount(); ix++ ){
	    final Clause cl = ctx.problem.clauses[ix];

	    terms[ix] = cl.pos.length + cl.neg.length;
	}
	ctx.terms = terms;
	ok = tryAssignments( ctx );

	if( traceSolver ){
	    System.err.println( "Made " + ctx.assumptions + " assumptions" );
	}
	// Return the result in an array that is exactly large enough to
	// contain the solutions.
	SATSolution res[] = new SATSolution[ctx.solutioncount];

	System.arraycopy( ctx.solutions, 0, res, 0, ctx.solutioncount );
	return res;
    }

    public static void main( String args[] ) throws java.io.IOException
    {
	if( args.length != 1 ){
	    System.err.println( "Exactly one filename argument required." );
	    System.exit( 1 );
	}
	File f = new File( args[0] );
	if( !f.exists() ){
	    System.err.println( "File does not exist: " + f );
	    System.exit( 1 );
	}
	SATProblem p = SATProblem.parseDIMACSStream( f );
	System.out.println( "Problem: " + p );
	SATSolution res[] = solveSystem( p );

	if( res.length == 1 ){
	    System.out.println( "There is 1 solution:" );
	}
	else if( res.length == 0 ){
	    System.out.println( "There are no solutions" );
	}
	else {
	    System.out.println( "There are " + res.length + " solutions" );
	}
	for( int ix=0; ix<res.length; ix++ ){
	    SATSolution s = res[ix];

	    System.out.println( "" + s );
	}
    }
}
