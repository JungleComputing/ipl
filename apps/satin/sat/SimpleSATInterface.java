// File: $Id$

interface SimpleSATInterface extends ibis.satin.Spawnable
{
    void solve( SATProblem p, int assignments[], int var ) throws SATResultException;
}
