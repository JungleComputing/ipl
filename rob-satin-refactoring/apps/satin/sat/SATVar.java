// $Id$

/**
 * A single variable of a SAT problem, with associated administration.
 * 
 * @author Kees van Reeuwijk
 * @version $Revision$
 */

final class SATVar implements java.io.Serializable, Comparable, Cloneable {
    private int ix;		// The index in the the original var array.
    private int pos[];	        // Clauses in which this var occurs as a pos.
    private int neg[];	        // Clauses in which this var occurs as a neg.

    /**
     * Constructs a new SATVar with the specified index and
     * empty occurence lists.
     */
    public SATVar( int ix )
    {
        this.ix = ix;
        pos = new int[0];
        neg = new int[0];
    }

    /** Constructs a new SATVar with the specified fields. */
    private SATVar( int ix, int pos[], int neg[] )
    {
        this.ix = ix;
        this.pos = pos;
        this.neg = neg;
    }

    /** Returns a clone of this SATVar. The pos and neg vectors are also
     * cloned.
     */
    public Object clone()
    {
        return new SATVar(
            ix,
            (int []) pos.clone(),
            (int []) neg.clone()
        );
    }

    public boolean equals( Object other )
    {
        SATVar o = (SATVar) other;

        return o != null &&
            ix == o.ix &&
            Helpers.areEqualArrays( pos, o.pos ) && 
            Helpers.areEqualArrays( neg, o.neg );
    }

    public int hashCode()
    {
        return ix + pos.length + neg.length;
    }

    /**
     * Registers the fact that clause 'cno' uses this variable as
     * a positive variable.
     */
    void registerPosClause( int cno ) {
        pos = Helpers.append( pos, cno );
    }

    /**
     * Registers the fact that clause 'cno' uses this variable as
     * a negative variable.
     */
    void registerNegClause( int cno ) {
        neg = Helpers.append( neg, cno );
    }

    void clearClauseRegister() {
        pos = new int[0];
        neg = new int[0];
    }

    int [] getPosClauses() { return pos; }
    int [] getNegClauses() { return neg; }

    /** Returns true iff the variable is used. */
    boolean isUsed() { return (pos != null && pos.length != 0) || (neg != null && neg.length != 0); }

    /** Returns true iff the variable only occurs in positive terms */
    boolean isPosOnly() { return (neg == null || neg.length == 0); }

    /** Returns true iff the variable only occurs in negative terms */
    boolean isNegOnly() { return (pos == null || pos.length == 0); }

    /** Returns the index of this variable. */
    int getIndex() { return ix; }

    /** Returns the number of clauses that this variable is used in. */
    int getUseCount() { return pos.length + neg.length; }

    /**
     * Returns the number of clauses that this variable is used in
     * as a positive term.
     */
    int getPosCount() { return pos.length; }

    /**
     * Returns the number of clauses that this variable is used in
     * as a negative term.
     */
    int getNegCount() { return neg.length; }

    /**
     * Returns the amount of information in the given uses of a variable.
     * @param v The list of clauses this variable occurs in.
     * @param clauses The clauses of the problem.
     * @return The amount of information.
     */
    static private float getInfo( int v[], Clause clauses[], ClauseReviewer r )
    {
        int sz = v.length;
        float info = 0.0f;

        for( int i=0; i<sz; i++ ){
            int cno = v[i];

            info += r.info( clauses[cno] );
        }
        return info;
    }

    /**
     * Returns the amount of information in a positive assignment of this
     * variable.
     * @param clauses The clauses of the problem.
     * @param r The reviewer that determines the info of a clause.
     * @return the amount of information in bit
     */
    float getPosInfo( Clause clauses[], ClauseReviewer r ) {
       return getInfo( pos, clauses, r );
    }

    /**
     * Returns the amount of information in a negative assignment of this
     * variable.
     * @param clauses The clauses of the problem.
     * @param r The reviewer that determines the info of a clause.
     * @return the amount of information in bit
     */
    float getNegInfo( Clause clauses[], ClauseReviewer r ) {
       return getInfo( neg, clauses, r );
    }

    public String toString()
    {
        return "(" + ix + ")";
    }
   
    // Note: this comparator imposes orderings that are inconsistent
    // with equals.
    public int compareTo( Object other )
    {
        SATVar co = (SATVar) other;
        int nthis = pos.length + neg.length;
        int nother = co.pos.length + co.neg.length;

        if( nthis>nother ){
            return -1;
        }
        if( nthis<nother ){
            return 1;
        }
        return 0;
    }
}
