// File: $Id$

/**
 * The Satin marker interface for the Breeder class. By implementing this
 * interface the class can be handled by the Satin divide-and-conquer parallel
 * execution framework.
 */
interface BreederInterface extends ibis.satin.Spawnable
{
    public int run( SATProblem pl[], Genes genes );
}
