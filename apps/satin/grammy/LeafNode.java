// File: $Id$
//
// A leaf node in the suffix tree.

import java.io.PrintStream;

public class LeafNode extends Node {
    int pos;

    LeafNode( int start, int len, int dist, int pos )
    {
        super( start, len, dist );
        this.pos = pos;
    }

    protected void add( short text[], int start, int length, int pos )
        throws VerificationException
    {
        throw new VerificationException( "Internal error, cannot add() on a leaf node." );
    }

    protected void print( PrintStream stream, short text[], int indent )
    {
        for( int i=0; i<indent; i++ ){
            stream.print( ' ' );
        }
        stream.println( "L" + pos + "[" + SuffixTree.buildString( text, start, length ) + "]d" + dist );
    }

    public String toString()
    {
        return "L" + pos + SuffixTree.buildSpan( start, length ) + "d" + dist;
    }

    protected Node getLongestRepeat()
    {
        System.err.println( "Internal error, cannot getLongestRepeat() on a leaf node." );
        return null;
    }

    public void test() throws VerificationException
    {
    }
}
