// File: $Id$

interface SimpleSATInterface extends ibis.satin.Spawnable
{
    void solve( SATProblem p, int assignments[], int varlist[], int varix ) throws SATResultException;
}
