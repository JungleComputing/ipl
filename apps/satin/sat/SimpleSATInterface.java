// File: $Id$

/** The Satin marker interface for the SimpleSATSolver class.
 * By implementing this interface the class can be handled by the
 * Satin divide-and-conquer parallel execution framework.
 */
interface SimpleSATInterface extends ibis.satin.Spawnable
{
    void solve( SATProblem p, int assignments[], int varlist[], int varix ) throws SATResultException;
}
