// File: $Id$

interface SimpleSATInterface extends ibis.satin.Spawnable
{
    SATSolution solve( SATProblem p, int assignments[], int var );
}
