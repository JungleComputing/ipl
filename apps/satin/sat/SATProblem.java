// File: $Id$
//
// Representation of a SAT problem in CNF form.

import java.io.*;

/**
 * A <code>SATProblem</code> object represents a SAT problem in
 * Conjunctive Normal Form (CNF).
 *
 * @author Kees van Reeuwijk
 * @version $Revision$
 */

public class SATProblem implements Cloneable, java.io.Serializable {
    /** The number of variables in the problem. */
    private int vars;

    /** The number of known variables of the problem. */
    private int knownVars;

    /** The number of clauses of the problem. */
    private int clauseCount;

    /** The clauses of the problem. */
    Clause clauses[];

    /** The variables of the problem. */
    private SATVar variables[];

    /** The number of deleted clauses. */
    private int deletedClauseCount;

    static final boolean trace_simplification = false;
    static final boolean trace_new_code = true;
    private int label = 0;

    /**
     * Constructs an empty SAT Problem.
     */
    private SATProblem()
    {
        vars = 0;
	knownVars = 0;
        createVarArray( 0 );
	clauses = new Clause[0];
    }

    /**
     * Construct a new SAT Problem with the given fields.
     */
    private SATProblem(
        int vars,
        int knownVars,
	int clauseCount,
	Clause clauses[],
	SATVar variables[],
	int deletedClauseCount,
	int label
    )
    {
        this.vars = vars;
        this.knownVars = knownVars;
        this.clauseCount = clauseCount;
        this.clauses = clauses;
        this.variables = variables;
        this.deletedClauseCount = deletedClauseCount;
        this.label = label;
    }

    public Object clone()
    {
	Clause cl[] = new Clause[clauseCount];
	SATVar vl[] = new SATVar[vars];

	for( int i=0; i<clauseCount; i++ ){
	    cl[i] = clauses[i];
	}
	for( int i=0; i<vars; i++ ){
	    vl[i] = variables[i];
	}
        return new SATProblem( 
	    vars,
	    knownVars,
	    clauseCount,
	    cl,
	    vl,
	    deletedClauseCount,
	    label
	);
    }

    /**
     * Constructs a SAT problem with the given number of variables and
     * the given list of clauses.
     *
     * @param v the number of variables in the problem
     * @param cl the clauses of the problem
     */
    public SATProblem( int v, Clause cl[] ){
	vars = v;
        createVarArray( v );
	clauses = cl;
	clauseCount = cl.length;
    }

    /**
     * Constructs a SAT problem with a given number of variables and
     * a given number of clauses.
     *
     * @param v the number of variables in the problem
     * @param n the number of clauses of the problem
     */
    private SATProblem( int v, int n ){
	vars = v;
	clauses = new Clause[n];
        createVarArray( v );
	clauseCount = 0;
    }

    /**
     * Sets {@link variables} to an array of variables with the given
     * length.
     *
     * @param n The number of variables.
     */
    private void createVarArray( int n )
    {
	variables = new SATVar[n];

	for( int i=0; i<n; i++ ){
	    variables[i] = new SATVar( i, i );
	}
    }

    /** Returns the number of variables used in the problem.  */
    public int getVariableCount()
    {
        return vars;
    }

    /** Returns the number of known variables in the problem.  */
    public int getKnownVariableCount()
    {
        return knownVars;
    }

    /** Returns the number of clauses of the problem.  */
    public int getClauseCount()
    {
	return clauseCount;
    }

    /**
     * Given a clause 'i', deletes it from the list.
     * @param i the index of the clause to delete
     */
    void deleteClause( int i )
    {
	clauseCount--;
	clauses[i] = clauses[clauseCount];
	deletedClauseCount++;
    }

    /**
     * Adds a new clause to the problem. The clause is constructed
     * from <em>copies</em> of the arrays of positive and negative variables
     * that are given as parameters.
     * @param pos the array of positive variables
     * @param possz the number of elements in <code>pos</code> to use
     * @param neg the array of negative variables
     * @param negsz the number of elements in <code>neg</code> to use
     * @return the label of the added clause
     */
    public int addClause( int pos[], int possz, int neg[], int negsz )
    {
	int apos[] = Helpers.cloneIntArray( pos, possz );
	int aneg[] = Helpers.cloneIntArray( neg, negsz );
	Clause cl = new Clause( apos, aneg, label++ );

        // First see if this clause is subsumed by an existing
	// one, or subsumes an existing one.
        for( int i=0; i<clauseCount; i++ ){
	    Clause ci = clauses[i];
	    
	    if( ci.isSubsumed( cl ) ){
		// The new clause is subsumed by an existing one,
		// don't bother to register it.
		if( trace_simplification ){
		    System.err.println( "New clause " + cl + " is subsumed by existing clause " + ci ); 
		}
		deletedClauseCount++;
		label--;	// Don't waste an unused label.
	        return -1;
	    }
	    if( cl.isSubsumed( ci ) ){
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

    /**
     * Adds a new clause to the problem. The clause is constructed
     * from <em>copies</em> of the arrays of positive and negative variables
     * that are given as parameters.
     * @param pos the array of positive variables
     * @param neg the array of negative variables
     * @return the label of the added clause
     */
    public int addClause( int pos[], int neg[] )
    {
	return addClause( pos, pos.length, neg, neg.length );
    }

    /**
     * Given a list of variables and a clause, registers all uses
     * of the variables in the clause.
     * @param cl the clause to register
     * @param clauseno the index of the clause to register
     */
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

    /**
     * Given a variable number, returns a vector of clauses in which this
     * variable occurs positively.
     *
     * @param var the variable to return the vector for
     * @return a vector of clause indices
     */
    public IntVector getPosClauses( int var )
    {
	return variables[var].getPosClauses();
    }

    /**
     * Given a variable number, returns a vector of clauses in which this
     * variable occurs negatively.
     *
     * @param var the variable to return the vector for
     * @return a vector of clause indices
     */
    public IntVector getNegClauses( int var )
    {
	return variables[var].getNegClauses();
    }

    /**
     * Given a clause index, return the label of that clause.
     * @param n the index of the clause
     * @return the label of the clause
     */
    public int getClauseLabel( int n ){
	return clauses[n].label;
    }

    /**
     * Given a list of assignments, returns the index of a clause that
     * is not satisfied, or -1 if they all are satisfied.
     * @param assignments the variable assignments
     * @return the index of an unsatisfied clause, or -1 if there are none
     */
    public int getUnsatisfied( int assignments[] )
    {
	for( int ix=0; ix<clauseCount; ix++ ){
	    if( !clauses[ix].isSatisfied( assignments ) ){
		return ix;
	    }
	}
	return -1;
    }

    /**
     * Given a list of assignments, returns true iff the assignments
     * satisfy this problem.
     * @param assignments the variable assignments
     * @return <code>true</code> iff the assignments satisfy the problem
     */
    public boolean isSatisfied( int assignments[] )
    {
	for( int ix=0; ix<clauseCount; ix++ ){
	    if( !clauses[ix].isSatisfied( assignments ) ){
		return false;
	    }
	}
	return true;
    }

    /**
     * Given a list of assignments, returns true iff the assignments
     * conflict with this problem. An assignment is conflicting if any
     * further assignments to unassigned variables cannot possibly satisfy
     * the problem.
     * @param assignments the variable assignments
     * @return <code>true</code> iff the assignments conflict with the problem
     */
    public boolean isConflicting( int assignments[] )
    {
	for( int ix=0; ix<clauseCount; ix++ ){
	    if( clauses[ix].isConflicting( assignments ) ){
		return true;
	    }
	}
	return false;
    }

    /**
     * Given a variable that is know to be true, propagates this
     * assignment.
     * @param var The variable that is known to be true.
     * @return <code>true</code> iff the propagation deleted any clauses.
     */
    private boolean propagatePosAssignment( int var )
    {
	boolean changed = false;

	int oldAssignment = variables[var].getAssignment();
	if( oldAssignment == 0 ){
	    System.err.println( "Cannot propagate positive assignment of variable " + var + ", since it contradicts a previous one" );
	    return false;
	}
	if( oldAssignment == 1 ){
	    // Already known.
	    return false;
	}
	knownVars++;
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

    /**
     * Given a variable that is know to be false, propagates this
     * assignment.
     * @param var The variable that is known to be true.
     * @return <code>true</code> iff the propagation deleted any clauses.
     */
    private boolean propagateNegAssignment( int var )
    {
	boolean changed = false;

	int oldAssignment = variables[var].getAssignment();
	if( oldAssignment == 1 ){
	    System.err.println( "Cannot propagate negative assignment of variable " + var + ", since it contradicts a previous one" );
	    return false;
	}
	if( oldAssignment == 0 ){
	    // Already known.
	    return false;
	}
	knownVars++;
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

    /** Removes null clauses from the clauses array. */
    private void compactClauses()
    {
	int ix = clauseCount;

	while( ix>0 ){
	    ix--;

	    if( clauses[ix] == null ){
	        deleteClause( ix );
	    }
	}
    }

    /** Optimizes the problem for solving. */
    void optimize()
    {
	boolean changed;

	do {
	    changed = false;

	    // For the moment, sort the clauses into shortest-first order.
	    java.util.Arrays.sort( clauses, 0, clauseCount );
	    for( int ix=0; ix<variables.length; ix++ ){
		SATVar v = variables[ix];

		v.clearClauseRegister();
	    }
	    for( int i=0; i<clauseCount; i++ ){
		registerClauseVariables( clauses[i], i );
	    }
	    for( int ix=0; ix<variables.length; ix++ ){
		SATVar v = variables[ix];

		if( !v.isUsed() ){
		    continue;
		}
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
		for( int j=i+1; j<clauseCount; j++ ){
		    Clause cj = clauses[j];

		    if( cj == null ){
			continue;
		    }
		    if( cl.isSubsumed( cj ) ){
		        if( trace_simplification ){
			    System.err.println( "Clause " + cl + " subsumes clause " + cj ); 
			}
			clauses[j] = null;
			changed = true;
		    }
		    else if( cj.isSubsumed( cl ) ){
		        if( trace_simplification ){
			    System.err.println( "Clause " + cj + " subsumes clause " + cl ); 
			}
			clauses[i] = null;
			changed = true;
			break;
		    }
		    else if( cl.joinClause( cj ) ){
		        if( trace_simplification ){
			    System.err.println( "Generalized clause: " + cl ); 
			}
			clauses[j] = null;
			changed = true;
		    }
		}
	    }
	    if( changed ){
		compactClauses();
	    }
	} while( changed );
    }

    /**
     * Given a list of assignments, specializes the SAT problem to incorporate
     * all definite assignments in the list.
     * @param assignments the assignments
     */
    private void specialize( int assignments[] )
    {
	boolean changed = true;

        for( int ix=0; ix<assignments.length; ix++ ){
	    int a = assignments[ix];

	    if( a != -1 ){
	        if( variables[ix].getAssignment() != -1 ){
		    // This is a new assignment. Propagate it.

		    if( a == 1 ){
			changed |= propagatePosAssignment( ix );
		    }
		    else {
			changed |= propagateNegAssignment( ix );
		    }
		}
	    }
	}
	if( changed ){
	    optimize();
	}
    }

    /**
     * Returns the initial assignment array for this problem.
     * @return an array of assignments for the variables of this problem
     */
    int [] getInitialAssignments()
    {
        int res[] = new int[vars];

	for( int ix=0; ix<res.length; ix++ ){
	    res[ix] = variables[ix].getAssignment();
	}
	return res;
    }

    /**
     * Returns a list of variables for this problem, ordered to be
     * most effective in solving the problem as fast as possible.
     * @return an ordered list of variables
     */
    int [] buildOrderedVarList()
    {
	int varix = 0;

	SATVar sorted_vars[] = new SATVar[vars];

	for( int i=0; i<vars; i++ ){
	    SATVar v = variables[i];

	    if( v != null || v.getAssignment() != -1 ){
		sorted_vars[varix++] = v;
	    }
	}
	java.util.Arrays.sort( sorted_vars, 0, varix );

	int res[] = new int[varix];
	for( int i=0; i<varix; i++ ){
	    // For the moment just use the unit mapping.
	    int ix = sorted_vars[i].getIndex();
	    res[i] = ix;
	}
	return res;
    }

    /**
     * Reads a CNF problem in DIMACS format from the given reader, and
     * returns a new SATProblem instance for that problem. 
     * See
     * {@link <a href="http://www.intellektik.informatik.tu-darmstadt.de/SATLIB/Benchmarks/SAT/satformat.ps">satformat.ps</a>}
     * for a description of the format.
     * @param in the reader that provides the stream to parse
     * @return the parsed problem
     */
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

    /**
     * A CNF problem parser. Given a problem in DIMACS format, returns
     * a SATProblem instance for it.
     * @param f the file to parse
     * @return the parsed problem
     */
    public static SATProblem parseDIMACSStream( File f ) throws java.io.IOException
    {
	InputStream s = new FileInputStream( f );
	return parseDIMACSStream( new BufferedReader( new InputStreamReader( s ) ) );
    }

    /**
     * Given an output stream, prints the problem to it in DIMACS format.
     * @param s the stream to print to
     */
    public void printDIMACS( PrintStream s )
    {
	s.println( "p cnf " + vars + " " + clauses.length );
	for( int ix=0; ix<clauses.length; ix++ ){
	    clauses[ix].printDIMACS( s );
	}
    }

    /** Returns a string representation of the SAT problem. */
    public String toString()
    {
	String res = "";

	for( int ix=0; ix<clauseCount; ix++ ){
	    res += "(" + clauses[ix] + ")";
	}
	return res;
    }
}

