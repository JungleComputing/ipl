/* $Id$ */

public interface KnapsackInterface extends ibis.satin.Spawnable {
    public Return spawn_try_it(int i, int tw, int av, int limw, int maxv,
            int[] values, int[] weights, byte[] s, byte[] opts);
}