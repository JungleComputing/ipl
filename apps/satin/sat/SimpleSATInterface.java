// File: $Id$

/** The Satin marker interface for the SimpleSATSolver class.
 * By implementing this interface the class can be handled by the
 * Satin divide-and-conquer parallel execution framework.
 */
interface SimpleSATInterface extends ibis.satin.Spawnable
{
    static class Context {
        SATProblem p;
	int varlist[];
    }

    public void solve( Context ctx, byte assignments[], int varix ) throws SATResultException;
}
