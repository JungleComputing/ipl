// File: $Id$

/** The Satin marker interface for the SATSolver class.
 * By implementing this interface the class can be handled by the
 * Satin divide-and-conquer parallel execution framework.
 */
/*static*/ class Context implements java.io.Serializable {
        SATProblem p;
	int varlist[];
    }

interface SATInterface extends ibis.satin.Spawnable
{

    public void solve( int level, Context ctx, int assignments[], int varix, boolean val ) throws SATResultException;
    public void agressiveSolve( int level, SATProblem p, int var, boolean val ) throws SATResultException;
}
