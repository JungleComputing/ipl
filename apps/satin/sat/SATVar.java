// $Id$
//
// Description of a single variable in the SAT solver.

class SATVar implements java.io.Serializable {
    private int label;
    private IntVector pos;	// Clauses in which this var occurs as a pos.
    private IntVector neg;	// Clauses in which this var occurs as a neg.

    public SATVar( int lbl )
    {
	label = lbl;
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
}
