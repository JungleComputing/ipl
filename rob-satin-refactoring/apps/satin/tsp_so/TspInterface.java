/* $Id$ */

interface TspInterface extends ibis.satin.Spawnable {
    public int spawn_tsp(int hops, byte[] path, int length, DistanceTable distance, Minimum minimum);
}
