// File: $Id$

/**
 * The context of the compression.
 */

class CompressContext implements java.io.Serializable {
    /**
     * For each hash code, the foremost position that matches this
     * hash code.
     */
    int heads[];

    int backrefs[];

    public CompressContext(int alsz, int textsize) {
        heads = new int[alsz];
        backrefs = new int[textsize];

        for (int i = 0; i < alsz; i++) {
            heads[i] = -1;
        }
    }

    private CompressContext(int heads[], int backrefs[]) {
        this.heads = heads;
        this.backrefs = backrefs;
    }

    public Object clone() {
        return new CompressContext((int[]) heads, (int[]) backrefs);
    }

    /**
     * Given a hash value 'c' and a position 'pos', registers the fact
     * that a string entry with this hash code is at the given position.
     */
    public void registerRef(int c, int pos) {
        backrefs[pos] = heads[c];
        heads[c] = pos;
    }
}