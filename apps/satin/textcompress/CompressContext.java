// File: $Id$

/**
 * The context of the compression.
 */

class CompressContext implements java.io.Serializable {
    /**
     * For each hash code of the current position, a list of backreferences
     * to previous occurences of that string.
     */
    int backref[][];

    public CompressContext( int alsz, int backrefs )
    {
        backref = new int[alsz][backrefs];
    }
}
