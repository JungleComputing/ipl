// File: $Id$

import java.io.PrintStream;

public class SuffixTree {
    short text[];
    Node root = new InternalNode();
    static final boolean traceAdding = false;
    static final short END = 256;

    static String buildString( short text[], int start, int length )
    {
        String s = "[" + start + ":" + length + "]";

        for( int i = 0; i<length; i++ ){
            short c = text[i+start];
            if( c>0 && c<255 ){
                s += (char) c;
            }
            else if( c == END ){
                s += "<end>";
            }
            else {
                s += "<" + c + ">";
            }
        }
        return s;
    }

    static String buildString( short text[] )
    {
        return buildString( text, 0, text.length );
    }

    abstract class Node {
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

        protected abstract void add( short text[], int start, int length, int pos );

        protected abstract void print( PrintStream stream, int indent );

        protected abstract short [] getLongestRepeat( short text[] );
    }

    class InternalNode extends Node {
        Node child;

        InternalNode( int start, int len, int dist )
        {
            super( start, len, dist );
            child = null;
        }

        /** Creates the root node. */
        InternalNode()
        {
            super( 0, 0, 0 );
        }

        protected void add( short text[], int start, int length, int pos )
        {
            if( traceAdding ){
                System.out.println( "Adding [" + buildString( text, start, length ) + "] @" + pos );
            }

            Node i = null;      // Insertion point.
            Node p = child;
            boolean match = false;

            while( p != null )
            {
                if( text[start] == text[p.start] ){
                    // Node p starts the same as the text we want to
                    // add, so we'll have to do some kind of merge.
                    match = true;
                    int l = Math.min( length, p.length );
                    int n = 1;

                    while( n<l ){
                        if( text[start+n] != text[p.start+n] ){
                            break;
                        }
                        n++;
                    }
                    if( n<p.length ){
                        // We must split up this node.
                        InternalNode nw = new InternalNode( p.start, n, p.dist );
                        nw.child = p;
                        nw.sister = p.sister;
			nw.parent = p.parent;
                        p.sister = null;
                        p.start += n;
                        p.length -= n;
			p.dist += n;
			p.parent = nw;
                        if( i == null ){
                            child = nw;
                        }
                        else {
                            i.sister = nw;
                        }
                        nw.add( text, start+n, length-n, pos );
                    }
                    else {
                        // This entire node matches.
                        p.add( text, start+p.length, length-p.length, pos );
                    }
                    break;
                }
                if( text[start] < text[p.start] ){
                    // We have walked past the insertion point, stop.
                    break;
                }
                i = p;
                p = p.sister;
            }

            if( !match ){
                // We must insert our text in the list of children at
                // insertion point 'i', or in front if i is null.
                Node newChild = new LeafNode( start, length, this.dist+this.length, pos );
                newChild.sister = p;
		newChild.parent = this;

                if( i == null ){
                    child = newChild;
                }
                else {
                    i.sister = newChild;
                }
            }
        }

        protected void print( PrintStream stream, int indent )
        {
            for( int i=0; i<indent; i++ ){
                stream.print( ' ' );
            }
            if( start>=0 ){
                stream.println( "[" + buildString( text, start, length ) + "]" );
            }
            Node n = child;

            while( n != null ){
                n.print( stream, indent+1 );
                n = n.sister;
            }
        }

        protected short [] getLongestRepeat( short text[] )
        {
            Node p = child;
            short res[];

            // Now do a second scan to find the longest sub-string.
            short max[] = null;
            p = child;
            while( p != null ){
                if( p instanceof InternalNode ){
                    short r[] = p.getLongestRepeat( text );

                    if( max == null || max.length<r.length ){
                        max = r;
                    }
                }
                p = p.sister;
            }

            if( max == null ){
                res = new short[length];
                System.arraycopy( text, start, res, 0, length );
            }
            else {
                res = new short[length+max.length];
                System.arraycopy( text, start, res, 0, length );
                System.arraycopy( max, 0, res, length, max.length );
            }
            return res;
        }
    }

    public class LeafNode extends Node {
        int pos;

        LeafNode( int start, int len, int dist, int pos )
        {
            super( start, len, dist );
            this.pos = pos;
        }

        protected void add( short text[], int start, int length, int pos )
        {
            System.err.println( "Internal error, cannot add() on a leaf node." );
        }

        protected void print( PrintStream stream, int indent )
        {
            for( int i=0; i<indent; i++ ){
                stream.print( ' ' );
            }
            stream.println( "[" + buildString( text, start, length ) + "] @" + pos );
        }

        protected short [] getLongestRepeat( short text[] )
        {
            System.err.println( "Internal error, cannot getLongestRepeat() on a leaf node." );
            return null;
        }
    }

    private short[] getLongestRepeat()
    {
        return root.getLongestRepeat( text );
    }

    private short[] buildShortArray( byte text[] )
    {
        short arr[] = new short[text.length+1];

        for( int i=0; i<text.length; i++ ){
            arr[i] = (short) text[i];
        }
        arr[text.length] = END;
        return arr;
    }

    private void buildTree( short text[] )
    {
        for( int i=0; i<text.length-1; i++ ){
            int l = (text.length-1)-i;

            root.add( text, i, l, i );
        }
    }

    private SuffixTree( short text[] )
    {
        this.text = text;

        buildTree( text );
    }

    SuffixTree( byte t[] )
    {
        this.text = buildShortArray( t );

        buildTree( text );
    }

    SuffixTree( String text )
    {
        this( text.getBytes() );
    }

    private void print( PrintStream s )
    {
        root.print( s, 0 );
    }

    public static void main( String args[] )
    {
        SuffixTree t = new SuffixTree( args[0] );

        t.print( System.out );

        short buf[] = t.getLongestRepeat();

        System.out.println( "Longest repeat: " + buildString( buf ) );
    }
}
