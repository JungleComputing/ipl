final class TranspositionTableEntry {
    static final byte LOWER_BOUND = 1;
    static final byte UPPER_BOUND = 2;
    static final byte EXACT_BOUND = 3;

    long tag; // index bits are redundant...
    short value;
    short bestChild;
    byte depth;
    boolean lowerBound;
}
