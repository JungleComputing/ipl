// File: $Id$
//
// Representation of a SAT problem in CNF form.

import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.File;
import java.io.StreamTokenizer;

class SATProblem implements java.io.Serializable {
    private int vars;		// Number of variables in the problem.
    Clause clauses[];		// The clauses of the problem.
    private SATVar variables[];	// The variables of the problem.
    private int clauseCount;	// The number of valid entries in `clauses'.
    private int deletedClauseCount;	// The number of deleted clauses.
    static final boolean trace_simplification = true;
    private int label = 0;

    private SATProblem()
    {
        vars = 0;
        createVarArray( 0 );
	clauses = new Clause[0];
    }

    public SATProblem( int v, Clause cl[] ){
	vars = v;
        createVarArray( v );
	clauses = cl;
	clauseCount = cl.length;
    }

    public SATProblem( int v, int n ){
	vars = v;
	clauses = new Clause[n];
        createVarArray( v );
	clauseCount = 0;
    }


    private void createVarArray( int n )
    {
	variables = new SATVar[n];

	for( int i=0; i<n; i++ ){
	    variables[i] = new SATVar( i );
	}
    }

    // Return the number of variables used in the problem.
    public int getVariableCount()
    {
        return vars;
    }

    public int getClauseCount()
    {
	return clauseCount;
    }

    // Given a clause 'i', delete it from the list.
    void deleteClause( int i )
    {
	clauseCount--;
	clauses[i] = clauses[clauseCount];
	deletedClauseCount++;
    }

    public int addClause( int pos[], int possz, int neg[], int negsz )
    {
	int apos[] = Helpers.cloneIntArray( pos, possz );
	int aneg[] = Helpers.cloneIntArray( neg, negsz );
	Clause cl = new Clause( apos, aneg, label++ );

        // First see if this clause is subsumed by an existing
	// one, or subsumes an existing one.
        for( int i=0; i<clauseCount; i++ ){
	    Clause ci = clauses[i];
	    
	    if( ci.isSubsumedClause( cl ) ){
		// The new clause is subsumed by an existing one,
		// don't bother to register it.
		if( trace_simplification ){
		    System.err.println( "New clause " + cl + " is subsumed by existing clause " + ci ); 
		}
		deletedClauseCount++;
		label--;	// Don't waste an unused label.
	        return -1;
	    }
	    if( cl.isSubsumedClause( ci ) ){
	        // The new clause subsumes an existing one. Remove
		// it, move the last clause to this slot, and
		// update clauseCount.
		if( trace_simplification ){
		    System.err.println( "New clause " + cl + " subsumes existing clause " + ci ); 
		}
		deleteClause( i );
	    }
	}
	if( clauseCount>=clauses.length ){
	    // Resize the clauses array. Even works for array of length 0.
	    Clause nw[] = new Clause[1+clauses.length*2];

	    System.arraycopy( clauses, 0, nw, 0, clauses.length );
	    clauses = nw;
	}
	int clauseno = clauseCount++;
	clauses[clauseno] = cl;
	return cl.label;
    }

    public int addClause( int pos[], int neg[] )
    {
	return addClause( pos, pos.length, neg, neg.length );
    }

    // Given a list of variables and a clause, register all uses
    // of the variables in the clause.
    private void registerClauseVariables( Clause cl, int clauseno )
    {
        int arr[] = cl.pos;

	for( int ix=0; ix<arr.length; ix++ ){
	    int var = arr[ix];

	    variables[var].registerPosClause( clauseno );
	}
	arr = cl.neg;
	for( int ix=0; ix<arr.length; ix++ ){
	    int var = arr[ix];

	    variables[var].registerNegClause( clauseno );
	}
    }

    public IntVector getPosClauses( int var )
    {
	return variables[var].getPosClauses();
    }

    public IntVector getNegClauses( int var )
    {
	return variables[var].getNegClauses();
    }

    public int getClauseLabel( int n ){
	return clauses[n].label;
    }

    // Given a list of assignments, return the index of a clause that
    // is not satisfied, or -1 if they all are satisfied.
    public int getUnsatisfied( int assignments[] )
    {
	for( int ix=0; ix<clauseCount; ix++ ){
	    if( !clauses[ix].isSatisfied( assignments ) ){
		return ix;
	    }
	}
	return -1;
    }

    public boolean isSatisfied( int assignments[] )
    {
	for( int ix=0; ix<clauseCount; ix++ ){
	    if( !clauses[ix].isSatisfied( assignments ) ){
		return false;
	    }
	}
	return true;
    }

    public boolean isConflicting( int assignments[] )
    {
	for( int ix=0; ix<clauseCount; ix++ ){
	    if( clauses[ix].isConflicting( assignments ) ){
		return true;
	    }
	}
	return false;
    }

    // Given a variable 'var' that we know is true, propagate this
    // assignment.
    boolean propagatePosAssignment( int var )
    {
	boolean changed = false;

	variables[var].setAssignment( 1 );
        for( int ix=0; ix<clauses.length; ix++ ){
	    Clause cl = clauses[ix];

	    if( cl == null ){
	        continue;
	    }
	    boolean sat = cl.propagatePosAssignment( var );
	    if( sat ){
	        // This clause is satisfied by the assignment. Remove it.
		clauses[ix] = null;
		changed = true;
		if( trace_simplification ){
		    System.err.println( "Clause " + cl + " is satisfied by var[" + var + "]=true" ); 
		}
	    }
	}
	return changed;
    }

    // Given a variable 'var' that we know is false, propagate this
    // assignment.
    boolean propagateNegAssignment( int var )
    {
	boolean changed = false;

	variables[var].setAssignment( 0 );
        for( int ix=0; ix<clauses.length; ix++ ){
	    Clause cl = clauses[ix];

	    if( cl == null ){
	        continue;
	    }
	    boolean sat = cl.propagateNegAssignment( var );
	    if( sat ){
	        // This clause is satisfied by the assignment. Remove it.
		clauses[ix] = null;
		changed = true;
		if( trace_simplification ){
		    System.err.println( "Clause " + cl + " is satisfied by var[" + var + "]=false" ); 
		}
	    }
	}
	return changed;
    }

    // Remove null clauses from the clauses array.
    void compactClauses()
    {
	int ix = clauseCount;

	while( ix>0 ){
	    ix--;

	    if( clauses[ix] == null ){
	        deleteClause( ix );
	    }
	}
    }

    // Optimize the problem for solving.
    void optimize()
    {
	boolean changed;

	do {
	    changed = false;

	    // For the moment, sort the clauses into shortest-first order.
	    java.util.Arrays.sort( clauses, 0, clauseCount );
	    for( int i=0; i<clauseCount; i++ ){
		registerClauseVariables( clauses[i], i );
	    }
	    for( int ix=0; ix<variables.length; ix++ ){
		SATVar v = variables[ix];

		if( v.isPosOnly() ){
		    // Variable 'v' only occurs in positive terms, we may as
		    // well assign to this variable, and propagate this
		    // assignment.
		    if( trace_simplification ){
			System.err.println( "Variable " + v + " only occurs as positive term" ); 
		    }
		    changed |= propagatePosAssignment( ix );
		}
		else if( v.isNegOnly() ){
		    // Variable 'v' only occurs in negative terms, we may as
		    // well assign to this variable, and propagate this
		    // assignment.
		    if( trace_simplification ){
			System.err.println( "Variable " + v + " only occurs as negative term" ); 
		    }
		    changed |= propagateNegAssignment( ix );
		}
		// TODO: a variable may occur never at all.
	    }
	    for( int i=0; i<clauseCount; i++ ){
	        Clause cl = clauses[i];

		if( cl == null ){
		    continue;
		}
		int var = cl.getPosUnitVar();
		if( var>=0 ){
		    if( trace_simplification ){
			System.err.println( "Propagating pos. unit clause " + cl ); 
		    }
		    changed |= propagatePosAssignment( var );
		    continue;
		}
		var = cl.getNegUnitVar();
		if( var>=0 ){
		    if( trace_simplification ){
			System.err.println( "Propagating neg. unit clause " + cl ); 
		    }
		    changed |= propagateNegAssignment( var );
		    continue;
		}
	    }
	    if( changed ){
		compactClauses();
	    }
	} while( changed );
    }

    // Return the initial assignment array for this problem.
    int [] getInitialAssignments()
    {
        int res[] = new int[vars];

	for( int ix=0; ix<res.length; ix++ ){
	    res[ix] = variables[ix].getAssignment();
	}
	return res;
    }

    // A CNF problem parser. Given a problem in DIMACS format,
    // return a SATProblem instance for it.
    //
    // See
    // www.intellektik.informatik.tu-darmstadt.de/SATLIB/Benchmarks/SAT/satformat.ps
    // for a description of the format.
    public static SATProblem parseDIMACSStream( Reader in ) throws java.io.IOException
    {
	int varcount = 0;
	int promisedClauseCount = 0;

	// We keep two arrays of size varcount to store terms of
	// the currently parsed clause. Once a clause is terminated, we
	// then copy them into arrays that are exactly large enough to
	// hold the terms.
	// Note that in theory these arrays are too small if a clause
	// contains repeats of the same term. That would be silly to do,
	// but is not explicitly forbidden by the DIMACS file format.
	int pos[] = null;
	int neg[] = null;
	int posix = 0;
	int negix = 0;
	boolean done = false;
	SATProblem res = null;

	StreamTokenizer tok = new StreamTokenizer( in );
	tok.eolIsSignificant( true );	// Because of 'c' and 'p' lines.
	tok.lowerCaseMode( false );	// Don't fold UC to LC.

	while( !done ){
	    int t = tok.nextToken();
	    boolean startOfLine = true;
	    boolean nextStartOfLine = false;

	    // System.out.println( "Token [" + tok.sval + "," + tok.nval + "] startOfLine=" + startOfLine );
	    switch( t ){
		case StreamTokenizer.TT_EOF:
		    done = true;
		    break;

		case StreamTokenizer.TT_EOL:
		    nextStartOfLine = true;
		    break;

		case StreamTokenizer.TT_NUMBER:
		    if( res == null ){
			System.err.println( tok.lineno() + ": clause given before `p' line" );
			return null;
		    }
		    if( tok.nval == 0 ){
			res.addClause( pos, posix, neg, negix );
			posix = 0;
			negix = 0;
		    }
		    else if( tok.nval<0 ){
			neg[negix++] = ((int) -tok.nval) - 1;
		    }
		    else {
			pos[posix++] = ((int) tok.nval) - 1;
		    }
		    break;

		case StreamTokenizer.TT_WORD:
		    // We got a word. This should only happen for
		    // 'c' and 'f' lines.
		    if( startOfLine ){
			if( tok.sval.equals( "c" ) ){
			    // A comment line.
			    // Eat all remaining tokens on this line.
			    do {
				t = tok.nextToken();
			    } while( t != StreamTokenizer.TT_EOF && t != StreamTokenizer.TT_EOL );
			    nextStartOfLine = true;
			}
			else if( tok.sval.equals( "p" ) ){
			    // A format line. Read the information.
			    if( res != null ){
				System.err.println( tok.lineno() + ": duplicate `p' line" );
				return null;
			    }
			    t = tok.nextToken();
			    if( t != StreamTokenizer.TT_WORD || !tok.sval.equals( "cnf" ) ){
				System.err.println( tok.lineno() + ": expected \"cnf\", but got \"" + tok.sval + "\"" );
				return null;
			    }
			    t = tok.nextToken();
			    if( t != StreamTokenizer.TT_NUMBER ){
				System.err.println( tok.lineno() + ": expected a number" );
				return null;
				
			    }
			    varcount = (int) tok.nval;
			    t = tok.nextToken();
			    if( t != StreamTokenizer.TT_NUMBER ){
				System.err.println( tok.lineno() + ": expected a number" );
				return null;
			    }
			    promisedClauseCount = (int) tok.nval;
			    t = tok.nextToken();
			    if( t != StreamTokenizer.TT_EOL ){
				System.err.println( tok.lineno() + ": spurious text after `f' line" );
				return null;
			    }
			    nextStartOfLine = true;
			    res = new SATProblem( varcount, promisedClauseCount );
			    pos = new int[varcount];
			    neg = new int[varcount];
			    posix = 0;
			    negix = 0;
			}
			else {
			    System.err.println( tok.lineno() + ": unexpected word \"" + tok.sval + "\" at start of line" );
			    return null;
			}
		    }
		    else {
			System.err.println( tok.lineno() + ": unexpected word \"" + tok.sval + "\"" );
			return null;
		    }
		    break;

		default:
		    System.err.println( tok.lineno() + ": unexpected character '" + (char) t + "'" );
		    break;
	    }
	    startOfLine = nextStartOfLine;
	}

	if( posix != 0 || negix != 0 ){
	    // There are some pending terms, flush the clause.
	    res.addClause( pos, posix, neg, negix );
	}
	if( res.clauseCount+res.deletedClauseCount<promisedClauseCount ){
	    System.out.println( "There are only " +  (res.clauseCount+res.deletedClauseCount) + " clauses, although " + promisedClauseCount + " were promised" );
	}
	res.optimize();
	return res;
    }

    // A CNF problem parser. Given a problem in DIMACS format,
    // return a SATProblem instance for it.
    public static SATProblem parseDIMACSStream( File f ) throws java.io.IOException
    {
	return parseDIMACSStream( new BufferedReader( new FileReader( f ) ) );
    }

    // Given an output stream, print the problem to it in DIMACS format.
    public void printDIMACS( PrintStream s )
    {
	s.println( "p cnf " + vars + " " + clauses.length );
	for( int ix=0; ix<clauses.length; ix++ ){
	    clauses[ix].printDIMACS( s );
	}
    }

    // Return a string representation of the problem.
    public String toString()
    {
	String res = "";

	for( int ix=0; ix<clauseCount; ix++ ){
	    res += "(" + clauses[ix] + ")";
	}
	return res;
    }
}

