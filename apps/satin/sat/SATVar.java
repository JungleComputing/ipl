// $Id$
//
// Description of a single variable in the SAT solver.

/** A single variable of a SAT problem, with associated administration. */

final class SATVar implements java.io.Serializable, Comparable, Cloneable {
    private int label;
    private int ix;		// The index in the the original var array.
    private IntVector pos;	// Clauses in which this var occurs as a pos.
    private IntVector neg;	// Clauses in which this var occurs as a neg.
    private int assignment = -1;	// 0 or 1 if it is a known variable.

    /** Constructs a new SATVar with the specified label and index. */
    public SATVar( int lbl, int ix )
    {
	label = lbl;
	this.ix = ix;
        pos = new IntVector();
	neg = new IntVector();
    }

    /** Constructs a new SATVar with the specified fields. */
    private SATVar( int label, int ix, IntVector pos, IntVector neg, int assignment )
    {
	this.label = label;
	this.ix = ix;
	this.pos = pos;
	this.neg = neg;
	this.assignment = assignment;
    }

    /** Returns a clone of this SATVar. The pos and neg vectors are also
     * cloned.
     */
    public Object clone()
    {
	return new SATVar(
	    label,
	    ix,
	    (IntVector) pos.clone(),
	    (IntVector) neg.clone(),
	    assignment
	);
    }

    /**
     * Registers the fact that clause 'cno' uses this variable as
     * a positive variable.
     */
    void registerPosClause( int cno ) { pos.add( cno ); }

    /**
     * Registers the fact that clause 'cno' uses this variable as
     * a negative variable.
     */
    void registerNegClause( int cno ) { neg.add( cno ); }

    void clearClauseRegister() { neg.clear(); pos.clear(); }

    IntVector getPosClauses() { return pos; }
    IntVector getNegClauses() { return neg; }

    /** Returns true iff the variable is used. */
    boolean isUsed() { return (pos != null && pos.size() != 0) || (neg != null && neg.size() != 0); }

    /** Returns true iff the variable only occurs in positive terms */
    boolean isPosOnly() { return neg == null || neg.size() == 0; }

    /** Returns true iff the variable only occurs in negative terms */
    boolean isNegOnly() { return pos == null || pos.size() == 0; }

    /** Registers assignment 'v' for this variable. */
    void setAssignment( int v ) { assignment = v; }

    /** Returns the assignment of this variable. */
    int getAssignment() { return assignment; }

    /** Returns the index of this variable. */
    int getIndex() { return ix; }

    /** Returns the number of clauses that this variable is used in. */
    int getUseCount() { return pos.size() + neg.size(); }

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
