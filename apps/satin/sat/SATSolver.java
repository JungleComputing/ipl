// File: $Id$

/**
 * A parallel SAT solver. Given a symbolic boolean equation in CNF, find a set
 * of assignments that make this equation true.
 * 
 * In this implementation the solver simply takes the first unassigned
 * variable, and tries both possible assignments. These tries can of course be
 * done in parallel, making this ideally suited for Satin. More subtle
 * approaches are definitely possible, though.
 * 
 * @author Kees van Reeuwijk
 * @version $Revision$
 */

import java.io.File;

public class SATSolver extends ibis.satin.SatinObject implements SATInterface, java.io.Serializable {
    static final boolean traceSolver = false;
    static final boolean printSatSolutions = true;
    static final boolean traceNewCode = true;
    static int label = 0;

    /** 
     * If there are less than this number of variables left, we consider
     * this a problem that is trivial enough to hand to the leaf solver.
     */
    static final int leafVariables = 0;

    /** 
     * For all combinations of the first `firstVariables' variables we
     * apply simplification. This essentially creates 2^firstVariables
     * sub-problems that are solved in their own context.
     */
    static final int firstVariables = 0;

    /**
     * A simple solver that is used when the remaining problem is too
     * small to justify expensive solvers.
     * The method throws a SATResultException if it finds a solution,
     * or terminates normally if it cannot find a solution.
     * @param level branching level
     * @param p the SAT problem to solve
     * @param assignments the current assignments
     * @param varlist the list of variables to branch on, ordered for efficiency
     * @param varix the next variable in <code>varlist</code> to branch on
     */
    private static void leafSolve(
	int level,
	SATProblem p,
	int varlist[],
	int assignments[],
	int varix,
	boolean val
    ) throws SATResultException
    {
	int var = varlist[varix];

	if( traceSolver ){
	    System.err.println( "ls" + level + ": Trying assignment var[" + var + "]=" + val + " varix=" + varix );
	}
	assignments[var] = val?1:0;

	if( p.isSatisfied( assignments ) ){
	    SATSolution s = new SATSolution( assignments );

	    if( traceSolver | printSatSolutions ){
		System.err.println( "ls" + level + ": found a solution: " + s );
	    }
	    throw new SATResultException( s );
	}
	if( p.isConflicting( assignments ) ){
	    if( traceSolver ){
		System.err.println( "ls" + level + ": found a conflict" );
	    }
	    assignments[var] = -1;
	    return;
	}

	varix++;
	// Search for an unassigned variable.
	while( varix<varlist.length ){
	    int nextvar = varlist[varix];

	    if( assignments[nextvar] == -1 ){
		break;
	    }
	    varix++;
	}
	if( varix>=varlist.length ){
	    // There are no variables left to assign, clearly there
	    // is no solution.
	    if( traceSolver ){
		System.err.println( "ls" + level + ": there are only " + varlist.length + " variables; nothing to branch on varix=" + varix );
	    }
	    assignments[var] = -1;
	    return;
	}

	leafSolve( level+1, p, varlist, assignments, varix, false );
	leafSolve( level+1, p, varlist, assignments, varix, true );
	assignments[var] = -1;
    }

    /**
     * The method that implements a Satin task.
     * The method throws a SATResultException if it finds a solution,
     * or terminates normally if it cannot find a solution.
     * @param level branching level
     * @param p the SAT problem to solve
     * @param ctx the changable context of the solver
     * @param assignments the current assignments
     * @param varix the next variable in <code>varlist</code> to assign
     * @param val the value to assign
     */
    public void solve(
	int level,
	SATProblem p,
	SATContext ctx,
	int var,
	boolean val
    ) throws SATResultException
    {
	if( traceSolver ){
	    System.err.println( "s" + level + ": trying assignment var[" + var + "]=" + val );
	}
	ctx.assignments[var] = val?1:0;
	int res;
	if( val ){
	    res = ctx.propagatePosAssignment( p, var );
	}
	else {
	    res = ctx.propagateNegAssignment( p, var );
	}
	if( res == -1 ){
	    // Propagation reveals a conflict.
	    if( traceSolver ){
		System.err.println( "s" + level + ": propagation found a conflict" );
	    }
	    return;
	}
	if( res == 1 ){
	    // Propagation reveals problem is satisfied.
	    SATSolution s = new SATSolution( ctx.assignments );

	    if( traceSolver | printSatSolutions ){
		System.err.println( "s" + level + ": propagation found a solution: " + s );
	    }
	    throw new SATResultException( s );
	}
	int nextvar = ctx.getDecisionVariable();
	if( nextvar<0 ){
	    // There are no variables left to assign, clearly there
	    // is no solution.
	    if( traceSolver ){
		System.err.println( "s" + level + ": there are only " + ctx.varlist.length + " variables; nothing to branch on" );
	    }
	    return;
	}

	// TODO: when should we switch to the leaf solver?
	if( false ){
	    leafSolve( level+1, p, ctx.varlist, ctx.assignments, 0, false );
	    leafSolve( level+1, p, ctx.varlist, ctx.assignments, 0, true );
	}
	else {
	    // We have variable 'nextvar' to branch on.
	    SATContext negctx = (SATContext) ctx.clone();
	    SATContext posctx = (SATContext) ctx.clone();
	    //if( ctx.posDominant( nextvar ) ){
	    if( ctx.posDominant( nextvar ) ){
		solve( level+1, p, negctx, nextvar, false );
		solve( level+1, p, posctx, nextvar, true );
	    }
	    else {
		solve( level+1, p, posctx, nextvar, true );
		solve( level+1, p, negctx, nextvar, false );
	    }
	    sync();
	}
    }

    /**
     * Agressively solves the specified SAT problem by creating a
     * specialized clone of the problem with the given variable
     * assigned the given value.
     * The method throws a SATResultException if it finds a solution,
     * or terminates normally if it cannot find a solution.
     * @param level the recursion level of the solution process
     * @param p the SAT problem to solve
     * @param var the variable to assign
     * @param val the value of <code>var</code>
     */
    public void agressiveSolve( int level, SATProblem old_p, int var, boolean val )
	throws SATResultException
    {
	if( traceSolver | traceNewCode ){
	    System.err.println( "as" + level + ": Trying assignment var[" + var + "]=" + val );
	}
	SATProblem p = (SATProblem) old_p.clone();
	p.propagateAssignment( var, val );
	p.optimize();
	if( traceSolver | traceNewCode ){
	    p.report( System.err );
	}
	if( p.isConflicting() ){
	    if( traceSolver | traceNewCode ){
		System.err.println( "as" + level + ": found a conflict" );
	    }
	    return;
	}
	if( p.isSatisfied() ){
	    SATSolution s = new SATSolution( p.buildInitialAssignments() );

	    if( traceSolver | printSatSolutions ){
		System.err.println( "as" + level + ": found a solution: " + s );
	    }
	    throw new SATResultException( s );
	}

	// Not yet solved, we'll have to do some work to solve it.

	if( level<firstVariables ){
	    // Do another level of agressive solving.
	    int nextvar = p.getMFUVariable();

	    if( nextvar<0 ){
		if( traceSolver | traceNewCode ){
		    System.err.println( "as" + level + ": no variable to branch on, done." );
		}
		return;
	    }
	    agressiveSolve( level+1, p, nextvar, false );
	    agressiveSolve( level+1, p, nextvar, true );
	    sync();
	}
	else {
	    // It's too expensive to do agressive solving, try something
	    // a bit more subtle.
	    SATContext ctx = new SATContext(
	        p.getClauseCount(),
		p.buildTermCounts(),
		p.buildPosClauses(),
		p.buildNegClauses()
	    );

	    ctx.varlist = p.buildOrderedVarList();

	    if( ctx.varlist.length == 0 ){
		// There are no variables left to assign, clearly there
		// is no solution.
		if( traceSolver | traceNewCode ){
		    System.err.println( "as" + level + ": nothing to branch on" );
		}
		return;
	    }

	    ctx.assignments = p.buildInitialAssignments();

	    if( leafVariables>=ctx.varlist.length ){
		leafSolve( level+1, p, ctx.varlist, ctx.assignments, 0, false );
		leafSolve( level+1, p, ctx.varlist, ctx.assignments, 0, true );
	    }
	    else {
		SATContext negctx = (SATContext) ctx.clone();
		int nextvar = ctx.getDecisionVariable();

		solve( level+1, p, negctx, nextvar, false );
		solve( level+1, p, ctx, nextvar, true );
		sync();
	    }
	}
    }

    /**
     * Given a SAT problem, returns a solution, or <code>null</code> if
     * there is no solution.
     */
    static SATSolution solveSystem( final SATProblem p )
    {
	SATSolution res = null;

	if( p.isConflicting() ){
	    return null;
	}
	if( p.isSatisfied() ){
	    return new SATSolution( p.buildInitialAssignments() );
	}
        SATSolver s = new SATSolver();

        // Now recursively try to find a solution.
	try {
	    int var = p.getMFUVariable();

	    if( traceSolver ){
		System.err.println( "Top level: branching on variable " + var );
	    }
	    if( var == -1 ){
		return null;
	    }
	    if( firstVariables == 0 ){
		// It's too expensive to do agressive solving, try something
		// a bit more subtle.
		SATContext ctx = new SATContext(
		    p.getClauseCount(),
		    p.buildTermCounts(),
		    p.buildPosClauses(),
		    p.buildNegClauses()
		);

		ctx.varlist = p.buildOrderedVarList();

		if( ctx.varlist.length == 0 ){
		    // There are no variables left to assign, clearly there
		    // is no solution.
		    if( traceSolver | traceNewCode ){
			System.err.println( "top: nothing to branch on" );
		    }
		    return null;
		}

		ctx.assignments = p.buildInitialAssignments();

		if( leafVariables>=ctx.varlist.length ){
		    leafSolve( 0, p, ctx.varlist, ctx.assignments, 0, false );
		    leafSolve( 0, p, ctx.varlist, ctx.assignments, 0, true );
		}
		else {
		    SATContext negctx = (SATContext) ctx.clone();
		    s.solve( 0, p, negctx, 0, false );
		    s.solve( 0, p, ctx, 0, true );
		}
	    }
	    else {
		s.agressiveSolve( 0, p, var, false );
		s.agressiveSolve( 0, p, var, true );
	    }
	    s.sync();
	}
	catch( SATResultException r ){
	    if( r.s == null ){
		System.err.println( "A null solution thrown???" );
	    }
	    res = r.s;
	    s.abort();
	}

	return res;
    }

    /** Allows execution of the class. */
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
	p.report( System.out );
	long startTime = System.currentTimeMillis();
	SATSolution res = solveSystem( p );

	long endTime = System.currentTimeMillis();
	double time = ((double) (endTime - startTime))/1000.0;

	System.out.println( "Time: " + time );
	if( res == null ){
	    System.out.println( "There are no solutions" );
	}
	else {
	    System.out.println( "There is a solution: " + res );
	}
    }
}
