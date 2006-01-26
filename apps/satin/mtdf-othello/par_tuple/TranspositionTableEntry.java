/* $Id$ */

class TranspositionTableEntry implements java.io.Serializable {
    Tag tag;

    short value;

    short bestChild;

    byte depth;

    boolean lowerBound;

    TranspositionTableEntry(Tag tag, short value, short bestChild,
            byte depth, boolean lowerBound) {
        this.tag = tag;
        this.value = value;
        this.bestChild = bestChild;
        this.depth = depth;
        this.lowerBound = lowerBound;
    }

    public void set(Tag tag, short value, short bestChild, byte depth,
            boolean lowerBound) {
        this.tag = tag;
        this.value = value;
        this.bestChild = bestChild;
        this.depth = depth;
        this.lowerBound = lowerBound;
    }
}
