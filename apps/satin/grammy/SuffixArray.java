// File: $Id$

import java.io.PrintStream;

public class SuffixArray {
    /** The buffer containing the compressed text. */
    private short text[];

    /** The number of elements in `text' that are relevant. */
    private int length;

    /** The indices in increasing alphabetical order of their suffix. */
    int indices[];

    /** For each position in `indices', the number of elements it
     * has in common with the previous entry, or -1 for element 0.
     * The commonality should not cause an overlap.
     */
    int commonality[];

    /** TODO, remove this encoding. */
    static final short END = 0;

    private short[] buildShortArray( byte text[] )
    {
        length = text.length;
        short arr[] = new short[length+1];

        for( int i=0; i<length; i++ ){
            arr[i] = (short) text[i];
        }
        arr[length] = END;
        return arr;
    }

    /** Returns the number of common characters in the two given spans. */
    private int commonLength( int i0, int i1 )
    {
	int n = 0;
	while( i0<length && i1<length && text[i0] == text[i1] ){
	    i0++;
	    i1++;
	    n++;
	}
	return n;
    }

    /** Returns true iff i0 refers to a smaller text than i1. */
    private boolean areCorrectlyOrdered( int i0, int i1 )
    {
        int n = commonLength( i0, i1 );
	if( text[i0] == END ){
	    // The sortest string is first, this is as it should be.
	    return true;
	}
	if( text[i1] == END ){
	    // The sortest string is last, this is not good.
	    return false;
	}
	return( text[i0]<text[i1] );
    }

    /** Build the suffix array and the commonality array. */
    private void buildArray()
    {
	indices = new int[length-1];
	commonality = new int[length-1];

	commonality[0] = -1;
	for( int i=0; i<indices.length; i++ ){
	    indices[i] = i;
	}
	int i = 0;

	// Now sort the indices. Uses `gnome sort' for the moment.
	// Since we need the computation anyway, also fill the commonality
	// array.
	while( i<indices.length ){
	    if( i == 0 ){
		i++;
	    }
	    else {
		int i0 = indices[i-1];
		int i1 = indices[i];
		int l = commonLength( i0, i1 );
		
		if( i0<i1 ){
		    commonality[i] = Math.min( l, i1-i0 );
		}
		else {
		    commonality[i] = Math.min( l, i0-i1 );
		}
		if( text[i0+l]<text[i1+l] ){
		    // Things are sorted, or we're at the start of the array,
		    // take a step forward.
		    i++;
		}
		else {
		    // Things are in the wrong order, swap them and step back.
		    int tmp = indices[i];
		    indices[i] = indices[i-1];
		    indices[--i] = tmp;
		}
	    }
	}
    }

    private SuffixArray( short text[] )
    {
        this.text = text;

        buildArray();
    }

    SuffixArray( byte t[] )
    {
        text = buildShortArray( t );

        buildArray();
    }

    SuffixArray( String text )
    {
        this( text.getBytes() );
    }

    static String buildString( short text[], int start, int length )
    {
        String s = "";
	int i = start;

        while( length>0  ){
            short c = text[i];
            if( c>0 && c<255 ){
                s += (char) c;
            }
            else if( c == END ){
		break;
            }
            else {
                s += "<" + c + ">";
            }
	    i++;
	    length--;
        }
        return s;
    }

    static String buildString( short text[], int start )
    {
	return buildString( text, start, text.length );
    }

    static String buildString( short text[] )
    {
        return buildString( text, 0 );
    }

    private void print( PrintStream s )
    {
	for( int i=0; i<indices.length; i++ ){
	    s.println( "" + indices[i] + " " + commonality[i] + " " + buildString( text, indices[i] ) );
	}
    }

    private void printMaxima( PrintStream s )
    {
	int max = 0;

	for( int i=1; i<indices.length; i++ ){
	    if( commonality[i]>commonality[max] ){
		max = i;
	    }
	}
	for( int i=1; i<indices.length; i++ ){
	    if( commonality[i] >= commonality[max] ){
		s.println( "maximum: " + indices[i] + " " + commonality[i] + " " + buildString( text, indices[i], commonality[i] ) );
	    }
	}
    }

    /**
     * Replaces the string at the given position and with
     * the given length with the given code. Also updates the administration.
     */
    private void replace( int pos, int len, short code )
    {
        
    }

    public void test() throws VerificationException
    {
        // Verify that the elements are in fact ordered, and that the
        // commonality entry is correct.
        for( int i=1; i<indices.length; i++ ){
            if( !areCorrectlyOrdered( i-1, i ) ){
                throw new VerificationException(
                    "suffix array is not incorrect order between " + (i-1) + " and " + i
                );
            }
        }
    }

    /** Apply one step in the folding process. */
    public void applyFolding()
    {
	// For the moment: nothing.
    }

    /** Returns a compressed version of the string represented by
     * this suffix array.
     */
    public ByteBuffer compress( int lookahead_depth )
    {
	applyFolding();
	return new ByteBuffer( text );
    }

    public static void main( String args[] )
    {
        try {
            SuffixArray t = new SuffixArray( args[0] );

            t.test();
            t.printMaxima( System.out );
        }
        catch( Exception x )
        {
            System.err.println( "Caught " + x );
            x.printStackTrace();
            System.exit( 1 );
        }
    }
}
