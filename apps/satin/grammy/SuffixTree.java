// File: $Id$

import java.io.PrintStream;

public class SuffixTree {
    short text[];
    Node root = new InternalNode();
    static final short END = 256;

    static String buildSpan( int start, int length )
    {
        return "[" + start + ":" + (start + length) + "]";
    }

    static String buildString( short text[], int start, int length )
    {
        String s = buildSpan( start, length );

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

    private Node getLongestRepeat()
    {
        return root.getLongestRepeat();
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

    private void buildTree( short text[] ) throws VerificationException
    {
        for( int i=0; i<text.length-1; i++ ){
            int l = (text.length-1)-i;

            root.add( text, i, l, i );
        }
    }

    private SuffixTree( short text[] ) throws VerificationException
    {
        this.text = text;

        buildTree( text );
    }

    SuffixTree( byte t[] ) throws VerificationException
    {
        this.text = buildShortArray( t );

        buildTree( text );
    }

    SuffixTree( String text ) throws VerificationException
    {
        this( text.getBytes() );
    }

    private void print( PrintStream s )
    {
        root.print( s, text, 0 );
    }

    public void test() throws VerificationException
    {
        root.test();
    }

    public static void main( String args[] )
    {
        try {
            SuffixTree t = new SuffixTree( args[0] );

            t.test();
            t.print( System.out );

            Node l = t.getLongestRepeat();

            System.out.println( "Longest repeat: " + l );
        }
        catch( Exception x )
        {
            System.err.println( "Caught " + x );
            x.printStackTrace();
            System.exit( 1 );
        }
    }
}
