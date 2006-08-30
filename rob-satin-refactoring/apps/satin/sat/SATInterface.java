// File: $Id$

/**
 * The Satin marker interface for the SATSolver class. By implementing this
 * interface the class can be handled by the Satin divide-and-conquer parallel
 * execution framework.
 */
interface SATInterface extends ibis.satin.Spawnable
{
    public void solve( int level, int localLevel, SATContext ctx, int varix, boolean val, boolean learnTuple ) throws SATException;
}
