// $Id$

/** A single clause in a symbolic Boolean expression. */

import java.io.PrintStream;

final class Clause implements java.io.Serializable, Comparable, Cloneable {
    int label;
    int pos[];		// The positive terms
    int neg[];		// The negative terms

    /**
     * Constructs a new clause with the given fields.
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
     * Returns a clone of this clause.
     * @return The clone of this clause.
     */
    public Object clone()
    {
	return new Clause( (int []) pos.clone(), (int []) neg.clone(), label );
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
     * Returns true iff 'l' contains 'n'.
     * We assume the list is sorted.
     * @param l the list to search in
     * @param n the value to search for
     * @return wether the list contains the element
     */
    static private boolean memberIntList( int l[], int n )
    {
	if( false ){
	    int from = 0;
	    int to = l.length;

	    while( from+1<to ){
		int mid = (from+to)/2;
		int v = l[mid];

		if( v == n ){
		    return true;
		}
		if( v<n ){
		    from = mid;
		}
		else {
		    to = mid;
		}
	    }
	}
	else {
	    for( int i=0; i<l.length; i++ ){
		if( l[i] == n ){
		    return true;
		}
	    }
	}
	return false;
    }

    /**
     * Returns true iff lb contains all symbols in la.
     * We assume that neither array contains duplicate elements.
     * @param la the reference array
     * @param lb the array that is being tested
     * @return wether la is a subset of lb
     */
    static private boolean isSubsetIntList( int la[], int lb[] )
    {
	int ixb = 0;

	if( lb.length<la.length ){
	    return false;
	}
	for( int ixa=0; ixa<la.length; ixa++ ){
	    int va = la[ixa];

	    while( ixb<lb.length && lb[ixb]<va ){
		ixb++;
	    }
	    if( ixb>=lb.length || lb[ixb]>va ){
		return false;
	    }
	    ixb++;
	}
	return true;
    }

    /**
     * Returns true iff clause 'cy' is subsumed by this clause.
     * @param cy the clause we compare to.
     */
    boolean isSubsumed( Clause cy )
    {
	return isSubsetIntList( this.pos, cy.pos ) &&
	    isSubsetIntList( this.neg, cy.neg );
    }

    /**
     * Given an array 'a', an array 'marks', and a flag 'flag', set the
     * given bits of all elements in 'marks' that have an index mentioned
     * in `a'.
     */
    private void setMarks( int marks[], int a[], int flag )
    {
        for( int ix=0; ix<a.length; ix++ ){
	    marks[a[ix]] |= flag;
	}
    }

    /**
     * Given two int arrays 'large' and 'small', where 'large' is exactly
     * one element longer than the small one, check whether all elements
     * in 'small' are in 'large'. Return the index of the one 
     * extra element, or -1 if there are more differences.
     * @param large the large array
     * @param small the small array
     * @param varCount The number of variables in this problem.
     */
    private int joinHalf( int large[], int small[], int varCount )
    {
	// We implement this function by marking all elements in `large'
	// and `small' in an array, and then searching for an element with
	// a special marking.
	int marks[] = new int[varCount];

	if( large.length != small.length+1 ){
	    System.err.println( "Large array has length " + large.length + ", but small array has length " + small.length );
	    return -1;
	}
	setMarks( marks, large, 1 );
	setMarks( marks, small, 2 );

	int special = -1;
	for( int ix=0; ix<varCount; ix++ ){
	    int v = marks[ix];

	    if( v == 2 ){
		// This element only occurs in `small', and not in `large'.
		// That can't be right.
	        return -1;
	    }
	    if( v == 1 ){
		// This element only occurs in `large', and not in `small'.
		// This is probably the one we're looking for.
		if( special != -1 ){
		    // There already is an element that only occurs in
		    // 'small', this can't be right.
		    return -1;
		}
		special = ix;
	    }
	}
	if( special != -1 ){
	    for( int ix=0; ix<large.length; ix++ ){
	        if( large[ix] == special ){
		    return ix;
		}
	    }
	}
	return -1;
    }

    /**
     * Registers that the specified variable is known to be true.
     * Returns true iff the clause is now satisfied.
     * @param var the variable that is known to be true
     * @return wether the clause is now satisfied
     */
    public boolean propagatePosAssignment( int var )
    {
        if( memberIntList( pos, var ) ){
	    // Clause is now satisfied.
	    return true;
	}
	// Now remove any occurence of 'var' in the 'neg' terms, since
	// it cannot satisfy the clause.
	for( int ix=0; ix<neg.length; ix++ ){
	    if( neg[ix] == var ){
		int negn[] = new int[neg.length-1];

		System.arraycopy( neg, 0, negn, 0, ix );
		System.arraycopy( neg, ix+1, negn, ix, (neg.length-1)-ix );
		neg = negn;
	    }
	}
	return false;
    }

    /**
     * Registers that the specified variable is known to be false.
     * Returns true iff the clause is now satisfied.
     * @param var the variable that is known to be false
     * @return wether the clause is now satisfied
     */
    public boolean propagateNegAssignment( int var )
    {
        if( memberIntList( neg, var ) ){
	    // Clause is now satisfied.
	    return true;
	}
	// Now remove any occurence of 'var' in the 'pos' terms, since
	// it cannot satisfy the clause.
	for( int ix=0; ix<pos.length; ix++ ){
	    if( pos[ix] == var ){
		int posn[] = new int[pos.length-1];

		System.arraycopy( pos, 0, posn, 0, ix );
		System.arraycopy( pos, ix+1, posn, ix, (pos.length-1)-ix );
		pos = posn;
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
    public boolean isSatisfied( byte assignments[] )
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

    /** Returns the total number of terms in this clause. */
    public int getTermCount()
    {
        return pos.length+neg.length;
    }

    /** Returns the total number of unassigned terms in the specified list. */
    private static int unassignedCount( int l[], byte assignment[] )
    {
        int res = 0;

        for( int i=0; i<l.length; i++ ){
            int v = l[i];

            if( assignment[v] == -1 ){
                res++;
            }
        }
        return res;
    }

    /** Returns the total number of unassigned terms in this clause. */
    public int getTermCount( byte assignment[] )
    {
        return unassignedCount( pos, assignment )+unassignedCount( neg, assignment );
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
     * Given two clauses and a variable, returns a new clause that 
     * is resolved on this variable.
     * @param c1 A clause.
     * @param c2 A clause.
     * @param v The variable to resolve on.
     * @return The new, resolved clause.
     */
    public static Clause resolve( Clause c1, Clause c2, int var )
    {
        // First, do a sanity check.
        if( 
            (memberIntList( c1.pos, var ) && memberIntList( c2.neg, var )) ||
            (memberIntList( c1.neg, var ) && memberIntList( c2.pos, var ))
        ){
        }
        else {
            System.err.println( "Cannot resolve " + c1 + " and " + c2 + " on v" + var );
        }
        int pos[] = new int[c1.pos.length+c2.pos.length];
        int neg[] = new int[c1.neg.length+c2.neg.length];
        int posno = 0;
        int negno = 0;

        int arr[] = c1.pos;
        for( int i=0; i<arr.length; i++ ){
            int v = arr[i];

            if( !memberIntList( c2.neg, v ) ){
                pos[posno++] = v;
            }
        }
        arr = c2.pos;
        for( int i=0; i<arr.length; i++ ){
            int v = arr[i];

            if( !memberIntList( c1.neg, v ) && !memberIntList( c1.pos, v ) ){
                pos[posno++] = v;
            }
        }
        arr = c1.neg;
        for( int i=0; i<arr.length; i++ ){
            int v = arr[i];

            if( !memberIntList( c2.pos, v ) ){
                neg[negno++] = v;
            }
        }
        arr = c2.neg;
        for( int i=0; i<arr.length; i++ ){
            int v = arr[i];

            if( !memberIntList( c1.pos, v ) && !memberIntList( c1.neg, v ) ){
                neg[negno++] = v;
            }
        }
        int newpos[] = Helpers.cloneIntArray( pos, posno );
        int newneg[] = Helpers.cloneIntArray( neg, negno );
        Helpers.sortIntArray( newpos );
        Helpers.sortIntArray( newneg );
        return new Clause( newpos, newneg, -1 );
    }

    /**
     * Given an array of assignments, return true iff this clause conflicts
     * with these assignments.
     * @param assignments the assignments
     * @return wether the assignments conflict with this clause
     */
    public boolean isConflicting( byte assignments[] )
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

    private static void registerInfo( int l[], float info[], float val )
    {
        for( int ix=0; ix<l.length; ix++ ){
            int v = l[ix];

            info[v] += val;
        }
    }

    /**
     * Given positive and negative info, updates the
     * counts with the info of this clause.
     * @param posinfo The information of a positive assignment of each variable.
     * @param neginfo The information of a negative assignment of each variable.
     * @param info The information to update with.
     */
    public void registerInfo( float posinfo[], float neginfo[], float info )
    {
        registerInfo( pos, posinfo, info );
        registerInfo( neg, neginfo, info );
    }

    private static void registerCount( int l[], int counts[] )
    {
        for( int ix=0; ix<l.length; ix++ ){
            int v = l[ix];

            counts[v]++;
        }
    }

    /**
     * Given positive and negative clause counts, updates the
     * counts with the values of this clause.
     * @param posclauses The positive clause count.
     * @param negclauses The negative clause count.
     */
    public void registerVariableCounts( int posclauses[], int negclauses[] )
    {
        registerCount( pos, posclauses );
        registerCount( neg, negclauses );
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
	    res += "-" + neg[ix];
	}
	res += " (" + label + ")";
	return res;
    }
}
