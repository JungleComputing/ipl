// $Id$
//
// A single clause in a symbolic Boolean expression

import java.io.PrintStream;

class Clause implements java.io.Serializable {
    int label;
    int pos[];		// The positive terms
    int neg[];		// The negative terms

    public Clause( int p[], int n[], int l )
    {
        pos = p;
	neg = n;
	label = l;
    }

    // Return true iff 'l' contains 'n'.
    static boolean memberIntList( int l[], int n )
    {
        for( int ix=0; ix<l.length; ix++ ){
	    if( l[ix] == n ){
		return true;
	    }
	}
	return false;
    }

    // Return true iff lb contains all symbols in la.
    static boolean isSubsetIntList( int la[], int lb[] )
    {
	for( int ix=0; ix<la.length; ix++ ){
	    if( !memberIntList( lb, la[ix] ) ){
		return false;
	    }
	}
	return true;
    }

    // Return true iff clause 'cy' is subsumed by this clause.
    boolean isSubsumedClause( Clause cy )
    {
	return isSubsetIntList( this.pos, cy.pos ) &&
	    isSubsetIntList( this.neg, cy.neg );
    }


    // Given an array of assignments, return true iff this clause is
    // satisfied by these assignments.
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

    // Given an array of assignments, return true iff this clause conflicts
    // with these assignments.
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

    // Given an output stream, print the clause to it in DIMACS format.
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
