// File: $Id$

/**
 * The Satin marker interface for the SATSolver class. By implementing this
 * interface the class can be handled by the Satin divide-and-conquer parallel
 * execution framework.
 */
interface SATInterface extends ibis.satin.Spawnable
{
    public void solve( int level, SATProblem p, SATContext ctx, int varix, boolean val ) throws SATResultException;
    public void agressiveSolve( int level, SATProblem p, int var, boolean val ) throws SATResultException;
}
