// $Id$
//
// Description of a single variable in the SAT solver.

class SATVar {
    IntVector pos;		// Clauses in which this var occurs as a pos.
    IntVector neg;		// Clauses in which this var occurs as a neg.

    public SATVar()
    {
        pos = new IntVector();
	neg = new IntVector();
    }

    // Register the fact that clause 'cno' uses this variable as
    // a positive variable.
    void registerPosClause( int cno )
    {
        pos.add( cno );
    }

    // Register the fact that clause 'cno' uses this variable as
    // a negative variable.
    void registerNegClause( int cno )
    {
        neg.add( cno );
    }
}
