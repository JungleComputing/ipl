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
    static final boolean traceSolver = false;
    static final boolean printSatSolutions = true;
    static int label = 0;

    public void solve( SATProblem p, int assignments[], int var ) throws SATResultException
    {
	if( p.isSatisfied( assignments ) ){
	    throw new SATResultException( new SATSolution( assignments ) );
	}
	if( p.isConflicting( assignments ) ){
	    return;
	}
	if( var>=p.getVariableCount() ){
	    // There are no variables left to assign, clearly there
	    // isn't a solution.
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

	// Start with a vector of unassigned variables.
	for( int ix=0; ix<assignments.length; ix++ ){
	    assignments[ix] = -1;
	}

        SimpleSATSolver s = new SimpleSATSolver();

        // Now recursively try to find a solution.
	try {
	    s.solve( p, assignments, 0 );
	    s.sync();
	}
	catch( SATResultException r ){
	    return r.s;
	}

	return null;
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
	SATSolution res = solveSystem( p );

	if( res == null ){
	    System.out.println( "There are no solutions" );
	}
	else {
	    System.out.println( "There is a solution: " + res );
	}
    }
}
