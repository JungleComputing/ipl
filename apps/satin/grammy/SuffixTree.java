// File: $Id$

import java.io.PrintStream;

public class SuffixTree {
    short text[];
    Node root = new InternalNode();
    static final boolean traceAdding = true;
    static final short END = 256;

    abstract class Node {
        int start;
        int length;
        Node sister;

        Node( int start, int len )
        {
            this.start = start;
            this.length = len;
            sister = null;
        }

        String buildString( short text[], int start, int length )
        {
            String s = "[" + start + ":" + length + "]";

            for( int i = 0; i<length; i++ ){
                short c = text[i+start];
                if( c<255 ){
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

        abstract void add( short text[], int start, int length, int pos );

        protected abstract void print( PrintStream stream, int indent );
    }

    class InternalNode extends Node {
        Node child;

        InternalNode( int start, int len )
        {
            super( start, len );
            child = null;
        }

        /** Creates the root node. */
        InternalNode()
        {
            super( 0, 0 );
        }

        void add( short text[], int start, int length, int pos )
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
                        InternalNode nw = new InternalNode( start, n );
                        nw.child = p;
                        nw.sister = p.sister;
                        p.sister = null;
                        p.start += n;
                        p.length -= n;
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
                Node newChild = new LeafNode( start, length, pos );
                newChild.sister = p;

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
    }

    public class LeafNode extends Node {
        int pos;

        LeafNode( int start, int len, int pos )
        {
            super( start, len );
            this.pos = pos;
        }

        void add( short text[], int start, int length, int pos )
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
    }
}
