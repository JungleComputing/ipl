// File: $Id$

/**
 * The Satin marker interface for the Breeder class. By implementing this
 * interface the class can be handled by the Satin divide-and-conquer parallel
 * execution framework.
 */
interface BreederInterface extends ibis.satin.Spawnable
{
    public void solve( int level, SATContext ctx, int varix, boolean val ) throws SATException;
}
