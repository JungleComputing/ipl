// File: $Id$

/**
 * The Satin marker interface for the SATSolver class. By implementing this
 * interface the class can be handled by the Satin divide-and-conquer parallel
 * execution framework.
 */
interface DPLLInterface extends ibis.satin.Spawnable
{
    public void solve( int level, int localLevel, SATProblem p, DPLLContext ctx, int varix, boolean val ) throws SATException;
}
