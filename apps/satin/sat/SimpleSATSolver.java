// File: $Id$

/** A simple but highly parallel SAT solver. Given a symbolic
 * boolean equation in CNF, find a set of assignments that make this
 * equation true.
 *
 * In this implementation the solver simply takes the first unassigned
 * variable, and tries both possible assignments. These tries can
 * of course be done in parallel, making this ideally suited for Satin.
 * More subtle approaches are definitely possible, though.
 */

import java.io.File;

public class SimpleSATSolver extends ibis.satin.SatinObject implements SimpleSATInterface, java.io.Serializable {
    private static final boolean traceSolver = false;
    private static final boolean printSatSolutions = true;
    private static final boolean printOptimizerStats = true;
    static int label = 0;

    /** If there are less than this number of variables left, we consider
     * this a problem that is trivial enough to hand to the leaf solver.
     */
    static final int leafVariables = 50;

    /** For all combinations of the first `firstVariables' variables we
     * apply simplification. This essentially creates 2^firstVariables
     * sub-problems that are solved in their own context.
     */
    static final int firstVariables = 4;

    /**
     * A simple solver that is used when the remaining problem is too
     * small to justify expensive solvers.
     * The method throws a SATResultException if it finds a solution,
     * or terminates normally if it cannot find a solution.
     * @param p the SAT problem to solve
     * @param assignments the current assignments
     * @param varlist the list of variables to branch on, ordered for efficiency
     * @param varix the next variable in <code>varlist</code> to branch on
     */
    public void leafSolve(
	SATProblem p,
	int varlist[],
	byte assignments[],
	int varix
    ) throws SATResultException
    {
	if( p.isSatisfied( assignments ) ){
	    SATSolution s = new SATSolution( assignments );

	    if( traceSolver | printSatSolutions ){
		System.err.println( "Found a solution: " + s );
	    }
	    throw new SATResultException( s );
	}
	if( p.isConflicting( assignments ) ){
	    if( traceSolver ){
		System.err.println( "Found a conflict" );
	    }
	    return;
	}
	if( varix>=varlist.length ){
	    // There are no variables left to assign, clearly there
	    // is no solution.
	    if( traceSolver ){
		System.err.println( "There are only " + p.getVariableCount() + " variables; nothing to branch on" );
	    }
	    return;
	}

	int var = varlist[varix];
	if( traceSolver ){
	    System.err.println( "leafSolver branches on variable " + var );
	    System.err.flush();
	}

	assignments[var] = 1;
	leafSolve( p, varlist, assignments, varix+1 );
	assignments[var] = 0;
	leafSolve( p, varlist, assignments, varix+1 );
	assignments[var] = -1;
    }

    /**
     * The method that implements a Satin task.
     * The method throws a SATResultException if it finds a solution,
     * or terminates normally if it cannot find a solution.
     * @param p the SAT problem to solve
     * @param assignments the current assignments
     * @param varlist the list of variables to branch on, ordered for efficiency
     * @param varix the next variable in <code>varlist</code> to branch on
     */
    public void solve(
	Context ctx,
	byte assignments[],
	int varix
    ) throws SATResultException
    {
	if( ctx.p.isSatisfied( assignments ) ){
	    SATSolution s = new SATSolution( assignments );

	    if( traceSolver | printSatSolutions ){
		System.err.println( "Found a solution: " + s );
	    }
	    throw new SATResultException( s );
	}
	if( ctx.p.isConflicting( assignments ) ){
	    if( traceSolver ){
		System.err.println( "Found a conflict" );
	    }
	    return;
	}
	if( varix>=ctx.varlist.length ){
	    // There are no variables left to assign, clearly there
	    // is no solution.
	    if( traceSolver ){
		System.err.println( "There are only " + ctx.p.getVariableCount() + " variables; nothing to branch on" );
	    }
	    return;
	}

	int var = ctx.varlist[varix];
	if( traceSolver ){
	    System.err.println( "Branching on variable " + var );
	    System.err.flush();
	}

	// We have variable 'var' to branch on.
	if( varix+leafVariables>=ctx.varlist.length ){
	    assignments[var] = 1;
	    leafSolve( ctx.p, ctx.varlist, assignments, varix+1 );
	    assignments[var] = 0;
	    leafSolve( ctx.p, ctx.varlist, assignments, varix+1 );
	    assignments[var] = -1;
	}
	else {
	    byte posassignments[] = (byte []) assignments.clone();
	    byte negassignments[] = (byte []) assignments.clone();
	    posassignments[var] = 1;
	    negassignments[var] = 0;
	    solve( ctx, posassignments, varix+1 );
	    solve( ctx, negassignments, varix+1 );
	    sync();
	}
    }

    /**
     * Given a SAT problem, returns a solution, or <code>null</code> if
     * there is no solution.
     */
    static SATSolution solveSystem( final SATProblem p )
    {
	byte assignments[] = p.buildInitialAssignments();
	int varlist[] = p.buildOrderedVarList();
	SATSolution res = null;
	int n = firstVariables;

        SimpleSATSolver s = new SimpleSATSolver();

	if( varlist.length<n ){
	    n = varlist.length;
	}
        // Now recursively try to find a solution.
	try {
	    boolean busy = true;

	    // Start with the null vector for the first variables.
	    for( int i=0; i<n; i++ ){
	        assignments[i] = 0;
	    }

	    // Now keep spawning solvers until we have tried all permutations
	    // of the first `firstVariables' variables.
	    do {
		if( traceSolver ){
		    System.err.println( "Starting recursive solver" );
		}
		Context ctx = new Context();

		ctx.p = p;
		ctx.varlist = varlist;
		s.solve( ctx, assignments, 0 );
		//System.err.println( "Solve finished??" );
		// res = null;

		// Calculate the next permutation to try.
		boolean carry = false;
		for( int i=0; i<n; i++ ){
		    if( assignments[i] == 0 ){
		        assignments[i] = 1;
			carry = false;
			break;
		    }
		    assignments[i] = 0;
		    carry = true;
		}
		if( carry ){
		    busy = false;
		}
	    } while( busy );
	    s.sync();
	}
	catch( SATResultException r ){
	    if( r.s == null ){
		System.err.println( "A null solution thrown???" );
	    }
	    res = r.s;
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
	p.optimize( printOptimizerStats );
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
