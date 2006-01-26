/* $Id$ */

public class TranspositionTableWrapper implements java.io.Serializable {
    public int rank;
    public int[] index, tag;
    public short[] value, bestChild;
    public byte[] depth;
    public boolean[] lowerBound;

    TranspositionTableWrapper(int rank, int[] index, int[] tag, short[] value,
            short[] bestChild, byte[] depth, boolean[] lowerBound) {
        this.rank = rank;
        this.index = index;
        this.tag = tag;
        this.value = value;
        this.bestChild = bestChild;
        this.depth = depth;
        this.lowerBound = lowerBound;
    }
}
