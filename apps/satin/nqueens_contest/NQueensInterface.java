/* $Id$ */

import ibis.satin.Spawnable;

interface NQueensInterface extends Spawnable {

    public long spawn_QueenNotInCorner(int[] board, int N, int spawnLevel, int y, int left, int down, int right, int mask, int lastmask, int sidemask, int bound1, int bound2, int topbit, int endbit);
    public long spawn_QueenInCorner(int y, int spawnLevel, int left, int down, int right, int bound1, int mask, int sizee);
}
