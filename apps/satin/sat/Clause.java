// $Id$

/** A single clause in a symbolic Boolean expression. */

import java.io.PrintStream;

class Clause implements java.io.Serializable, Comparable {
    int label;
    int pos[];		// The positive terms
    int neg[];		// The negative terms

    /**
     * @param p the positive terms of the clause
     * @param n the negative terms of the clause
     * @param l the labels of the clause
     */
    public Clause( int p[], int n[], int l )
    {
        pos = p;
	neg = n;
	label = l;
    }

    /**
     * Note: this comparator imposes orderings that are inconsistent
     * with equals.
     */
    public int compareTo( Object other )
    {
	Clause co = (Clause) other;
	int nthis = pos.length + neg.length;
	int nother = co.pos.length + co.neg.length;

	if( nthis>nother ){
	    return 1;
	}
	if( nthis<nother ){
	    return -1;
	}
	return 0;
    }

    /**
     * Return true iff 'l' contains 'n'.
     */
    static boolean memberIntList( int l[], int n )
    {
        for( int ix=0; ix<l.length; ix++ ){
	    if( l[ix] == n ){
		return true;
	    }
	}
	return false;
    }

    /**
     * Return true iff lb contains all symbols in la.
     */
    static boolean isSubsetIntList( int la[], int lb[] )
    {
	for( int ix=0; ix<la.length; ix++ ){
	    if( !memberIntList( lb, la[ix] ) ){
		return false;
	    }
	}
	return true;
    }

    /**
     * Return true iff clause 'cy' is subsumed by this clause.
     */
    boolean isSubsumedClause( Clause cy )
    {
	return isSubsetIntList( this.pos, cy.pos ) &&
	    isSubsetIntList( this.neg, cy.neg );
    }

    /**
     * Return true iff variable 'v' occurs as a positive term in this clause.
     */
    boolean occursPos( int var )
    {
        return memberIntList( pos, var );
    }

    /**
     * Return true iff variable 'v' occurs as a negative term in this clause.
     */
    boolean occursNeg( int var )
    {
        return memberIntList( neg, var );
    }

    /**
     * Given a variable var, register the fact that this variable is
     * known to be true. Return true iff the clause is now satisfied.
     * @param var the variable that is known to be true
     * @return wether the clause is now satisfied
     */
    boolean propagatePosAssignment( int var )
    {
        if( memberIntList( pos, var ) ){
	    // Clause is now satisfied.
	    return true;
	}
	// Now remove any occurence of 'var' in the 'neg' terms, since
	// they cannot satisfy the clause.
	for( int ix=0; ix<neg.length; ix++ ){
	    if( neg[ix] == var ){
		int nneg[] = Helpers.cloneIntArray( neg, neg.length-1 );
		if( ix<nneg.length ){
		    nneg[ix] = neg[neg.length-1];
		}
		neg = nneg;
	    }
	}
	return false;
    }

    /**
     * Given a variable var, register the fact that this variable is
     * known to be true. Return true iff the clause is now satisfied.
     * @param var the variable that is known to be false
     * @return wether the clause is now satisfied
     */
    boolean propagateNegAssignment( int var )
    {
        if( memberIntList( neg, var ) ){
	    // Clause is now satisfied.
	    return true;
	}
	// Now remove any occurence of 'var' in the 'pos' terms, since
	// they cannot satisfy the clause.
	for( int ix=0; ix<pos.length; ix++ ){
	    if( pos[ix] == var ){
		int npos[] = Helpers.cloneIntArray( pos, pos.length-1 );
		if( ix<npos.length ){
		    npos[ix] = pos[pos.length-1];
		}
		pos = npos;
	    }
	}
	return false;
    }

    /**
     * Given an array of assignments, return true iff this clause is
     * satisfied by these assignments.
     * @param assignments the assignments
     * @return wether the clause is now satisfied
     */
    public boolean isSatisfied( int assignments[] )
    {
	for( int ix=0; ix<pos.length; ix++ ){
	    int v = pos[ix];

	    if( assignments[v] == 1 ){
		return true;
	    }
	}
	for( int ix=0; ix<neg.length; ix++ ){
	    int v = neg[ix];

	    if( assignments[v] == 0 ){
		return true;
	    }
	}
	return false;
    }

    /**
     * If this clause is not a positive unit clause, return -1,
     * else return the variable that constitutes this clause.
     * @return The variable if this is a positive unit clause, or else -1.
     */
    public int getPosUnitVar()
    {
        if( neg.length != 0 ){
	    return -1;
	}
        if( pos.length != 1 ){
	    return -1;
	}
	return pos[0];
    }

    /**
     * If this clause is not a negative unit clause, return -1,
     * else return the variable that constitutes this clause.
     * @return The variable if this is a negative unit clause, or else -1.
     */
    public int getNegUnitVar()
    {
        if( pos.length != 0 ){
	    return -1;
	}
        if( neg.length != 1 ){
	    return -1;
	}
	return neg[0];
    }

    /**
     * Given an array of assignments, return true iff this clause conflicts
     * with these assignments.
     * @param assignments the assignments
     * @return wether the assignments conflict with this clause
     */
    public boolean isConflicting( int assignments[] )
    {
	// Search for any term of the clause that has an agreeing assignment
	// or is uncommitted.
	for( int ix=0; ix<pos.length; ix++ ){
	    int v = pos[ix];

	    if( assignments[v] != 0 ){
		return false;
	    }
	}
	for( int ix=0; ix<neg.length; ix++ ){
	    int v = neg[ix];

	    if( assignments[v] != 1 ){
		return false;
	    }
	}
	return true;
    }

    /**
     * Given an output stream, print the clause to it in DIMACS format.
     * @param s the stream to print to
     */
    public void printDIMACS( PrintStream s )
    {
	for( int ix=0; ix<pos.length; ix++ ){
	    s.print( (pos[ix]+1) + " " );
	}
	for( int ix=0; ix<neg.length; ix++ ){
	    s.print( "-" + (neg[ix]+1) + " " );
	}
	s.println( "0" );
    }

    /** Returns a string representation of this clause. */
    public String toString()
    {
        String res = "";
	boolean first = true;

	for( int ix=0; ix<pos.length; ix++ ){
	    if( !first ){
	        res += " ";
	    }
	    else {
	        first = false;
	    }
	    res += pos[ix];
	}
	for( int ix=0; ix<neg.length; ix++ ){
	    if( !first ){
	        res += " ";
	    }
	    else {
	        first = false;
	    }
	    res += "!" + neg[ix];
	}
	res += " (" + label + ")";
	return res;
    }
}
