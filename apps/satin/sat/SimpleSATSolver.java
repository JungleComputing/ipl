// File: $Id$
//
// An extremely simple and highly parallel SAT solver. Given a symbolic
// boolean equation in CNF, find a set of assignments that make this
// equation true.
//
// In this implementation the solver simply takes the first unassigned
// variable, and tries both possible assignments. These tries can
// of course be done in parallel, making this ideally suited for Satin.
// More subtility is definitely possible, though.

import java.io.File;

public class SimpleSATSolver implements SimpleSATInterface, java.io.Serializable {
    static final boolean traceSolver = false;
    static final boolean printSatSolutions = true;
    static int label = 0;

    public SATSolution solve( SATProblem p, int assignments[], int var )
    {
	if( p.isSatisfied( assignments ) ){
	    return new SATSolution( assignments );
	}
	if( p.isConflicting( assignments ) ){
	    return null;
	}
	if( var>=p.getVariableCount() ){
	    // There are no variables left to assign, clearly there
	    // isn't a solution.
	    //
	    // TODO: detect conflicting assignments so that the recursion
	    // can be terminated earlier.
	    return null;
	}

	// We have variable 'var' to branch on.
	int posassignments[] = (int []) assignments.clone();
	int negassignments[] = (int []) assignments.clone();
	posassignments[var] = 1;
	negassignments[var] = 0;
	SATSolution posres = solve( p, posassignments, var+1 );
	SATSolution negres = solve( p, negassignments, var+1 );
	// sync();
	if( posres != null ){
	    return posres;
	}
	return negres;
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
	SATSolution res = s.solve( p, assignments, 0 );

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
	SATSolution res = solveSystem( p );

	if( res == null ){
	    System.out.println( "There are no solutions" );
	}
	else {
	    System.out.println( "There is a solution: " + res );
	}
    }
}
