// File: $Id$
//
// An extremely simple and highly parallel SAT solver. Given a symbolic
// boolean equation in CNF, find a set of assignments that make this
// equation true.
//
// In this implementation the solver simply takes the first unassigned
// variable, and tries both possible assignments. These tries can
// of course be done in parallel, making this ideally suited for Satin.
// More subtle approaches are definitely possible, though.

import java.io.File;

public class SimpleSATSolver extends ibis.satin.SatinObject implements SimpleSATInterface, java.io.Serializable {
    static final boolean traceSolver = true;
    static final boolean printSatSolutions = true;
    static int label = 0;

    public void solve( SATProblem p, int assignments[], int var ) throws SATResultException
    {
	if( traceSolver ){
	    System.err.println( "Branching on variable " + var );
	    System.err.flush();
	}
	if( p.isSatisfied( assignments ) ){
	    if( traceSolver ){
		System.err.println( "Found a solution" );
	    }
	    throw new SATResultException( new SATSolution( assignments ) );
	}
	if( p.isConflicting( assignments ) ){
	    if( traceSolver ){
		System.err.println( "Found a conflict" );
	    }
	    return;
	}
	if( var>=p.getVariableCount() ){
	    // There are no variables left to assign, clearly there
	    // isn't a solution.
	    if( traceSolver ){
		System.err.println( "There are only " + p.getVariableCount() + " variables; nothing to branch on" );
	    }
	    return;
	}

	// We have variable 'var' to branch on.
	int posassignments[] = (int []) assignments.clone();
	int negassignments[] = (int []) assignments.clone();
	posassignments[var] = 1;
	negassignments[var] = 0;
	solve( p, posassignments, var+1 );
	solve( p, negassignments, var+1 );
	sync();
    }

    // Given a list of symbolic clauses, produce a list of solutions.
    static SATSolution solveSystem( final SATProblem p )
    {
	int assignments[] = new int[p.getVariableCount()];
	SATSolution res = null;

	// Start with a vector of unassigned variables.
	for( int ix=0; ix<assignments.length; ix++ ){
	    assignments[ix] = -1;
	}

        SimpleSATSolver s = new SimpleSATSolver();

        // Now recursively try to find a solution.
	try {
	    if( traceSolver ){
		System.err.println( "Starting recursive solver" );
	    }
	    s.solve( p, assignments, 0 );
	    //System.err.println( "Solve finished??" );
	    // res = null;
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
	System.out.println( "Problem has " + p.getVariableCount() + " variables and " + p.getClauseCount() + " clauses" );
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
