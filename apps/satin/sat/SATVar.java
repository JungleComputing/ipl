// $Id$
//
// Description of a single variable in the SAT solver.

/** A single variable of a SAT problem. */

class SATVar implements java.io.Serializable, Comparable {
    private int label;
    private int ix;		// The index in the the original var array.
    private IntVector pos;	// Clauses in which this var occurs as a pos.
    private IntVector neg;	// Clauses in which this var occurs as a neg.
    private int assignment = -1;	// 0 or 1 if it is a known variable.

    public SATVar( int lbl, int ix )
    {
	label = lbl;
	this.ix = ix;
        pos = new IntVector();
	neg = new IntVector();
    }

    // Register the fact that clause 'cno' uses this variable as
    // a positive variable.
    void registerPosClause( int cno ) { pos.add( cno ); }

    // Register the fact that clause 'cno' uses this variable as
    // a negative variable.
    void registerNegClause( int cno ) { neg.add( cno ); }

    IntVector getPosClauses() { return pos; }
    IntVector getNegClauses() { return neg; }

    // Return true iff the variable only occurs in positive terms
    boolean isPosOnly() { return neg == null || neg.size() == 0; }

    // Return true iff the variable only occurs in negative terms
    boolean isNegOnly() { return pos == null || pos.size() == 0; }

    // Register assignment 'v' for this variable.
    void setAssignment( int v ) { assignment = v; }

    // Get the assignment of this variable.
    int getAssignment() { return assignment; }

    int getIndex() { return ix; }

    public String toString()
    {
        return "(" + label + ")";
    }
   
    // Note: this comparator imposes orderings that are inconsistent
    // with equals.
    public int compareTo( Object other )
    {
	SATVar co = (SATVar) other;
	int nthis = pos.size() + neg.size();
	int nother = co.pos.size() + co.neg.size();

	if( nthis>nother ){
	    return -1;
	}
	if( nthis<nother ){
	    return 1;
	}
	return 0;
    }
}
