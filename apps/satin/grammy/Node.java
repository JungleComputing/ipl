// File: $Id$
//
// The abstract parent  of the different types of node in the suffix tree.

import java.io.PrintStream;

abstract class Node implements Configuration {
    /** The first character of the span this node represents. */
    int start;

    /** The length of the span this node represents. */
    int length;

    /** The distance in characters to the root of this node. */
    int dist;

    /** The next node with the same parent, or null if there isn't one. */
    Node sister;

    /** The parent of this node, or null if it is the root. */
    Node parent;

    Node( int start, int len, int dist )
    {
        this.start = start;
        this.length = len;
        this.dist = dist;
        sister = null;
        parent = null;
    }

    protected abstract void add( short text[], int start, int length, int pos )
        throws VerificationException;

    protected abstract void print( PrintStream stream, short text[], int indent );

    protected abstract Node getLongestRepeat();

    public abstract void test() throws VerificationException;
}
