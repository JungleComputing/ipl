// File: $Id$
//
// Representation of a problem in CNF form.

import java.io.Reader;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.File;
import java.io.StreamTokenizer;

class SATProblem {
    int vars;		 	// Number of variables in the problem.
    Clause clauses[];		// The clauses of the problem.
    private SATVar variables[];		// The variables of the problem.
    int clauseCount;		// The number of valid entries in `clauses'.
    int label = 0;

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

    // Given an array 'a' and a size 'sz', create a new array of size 'sz'
    // that contains the first 'sz' elements of 'a'.
    private static int[] cloneIntArray( int a[], int sz )
    {
        int res[] = new int[sz];

	System.arraycopy( a, 0, res, 0, sz );
	return res;
    }

    // Return the number of variables used in the problem.
    public int getVariableCount()
    {
        return vars;
    }

    public void addClause( int pos[], int possz, int neg[], int negsz )
    {
	int apos[] = cloneIntArray( pos, possz );
	int aneg[] = cloneIntArray( neg, negsz );
	Clause cl = new Clause( apos, aneg, label++ );

	if( clauseCount>=clauses.length ){
	    // Resize the clauses array. Even works for array of length 0.
	    Clause nw[] = new Clause[1+clauses.length*2];

	    System.arraycopy( clauses, 0, nw, 0, clauses.length );
	    clauses = nw;
	}
	clauses[clauseCount++] = cl;
    }

    // Given a list of variables and a clause, register all uses
    // of the variables in the clause.
    void registerClauseVariables( Clause cl, int clauseno )
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

    // Given a list of variables and a list of clauses, register all uses
    // of the variables in these clauses.
    void registerClauseVariables( Clause clauses[] )
    {
        for( int ix=0; ix<clauses.length; ix++ ){
	    Clause cl = clauses[ix];

	    registerClauseVariables( cl, ix );
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
	if( res.clauseCount<promisedClauseCount ){
	    System.out.println( "There are only " +  res.clauseCount + " clauses, although " + promisedClauseCount + " were promised" );
	}
	return res;
    }

    // A CNF problem parser. Given a problem in DIMACS format,
    // return a SATProblem instance for it.
    public static SATProblem parseDIMACSStream( File f ) throws java.io.IOException
    {
	return parseDIMACSStream( new FileReader( f ) );
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

	for( int ix=0; ix<clauses.length; ix++ ){
	    res += "(" + clauses[ix] + ")";
	}
	return res;
    }
}

