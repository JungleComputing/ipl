// File: $Id$

import java.io.PrintStream;

public class SuffixArray implements Configuration, Magic {
    /** The buffer containing the compressed text. */
    private short text[];

    /** The number of elements in `text' that are relevant. */
    private int length;

    /** The next grammar rule number to hand out. */
    private short nextcode = FIRSTCODE;

    /** The indices in increasing alphabetical order of their suffix. */
    int indices[];

    /** For each position in `indices', the number of elements it
     * has in common with the previous entry, or -1 for element 0.
     * The commonality should not cause an overlap.
     */
    int commonality[];

    private short[] buildShortArray( byte text[] )
    {
        length = text.length;
        short arr[] = new short[length];

        for( int i=0; i<length; i++ ){
            arr[i] = (short) text[i];
        }
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
	if( i0+n>=length ){
	    // The sortest string is first, this is as it should be.
	    return true;
	}
	if( i1+n>=length ){
	    // The sortest string is last, this is not good.
	    return false;
	}
	return( text[i0]<text[i1] );
    }

    /** Sorts the administration arrays to implement ordering. */
    private void sort()
    {
	int i = 0;

	// Now sort the indices. Uses `gnome sort' for the moment.
	// Since we need the computation anyway, also fill the commonality
	// array.
	while( i<length ){
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
		if( (i0+l>=length) || (i1+l<length && (text[i0+l]<text[i1+l])) ){
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

    /** Builds the suffix array and the commonality array. */
    private void buildArray()
    {
	indices = new int[length];
	commonality = new int[length];

	commonality[0] = -1;
	for( int i=0; i<indices.length; i++ ){
	    indices[i] = i;
	}

        sort();
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

    String buildString( int start, int len )
    {
        String s = "";
	int i = start;

        while( len>0 && i<this.length  ){
            int c = (text[i] & 0xFFFF);

            if( c<255 ){
                s += (char) c;
            }
            else {
                s += "<" + c + ">";
            }
	    i++;
	    len--;
        }
        return s;
    }

    String buildString( int start )
    {
	return buildString( start, length );
    }

    String buildString()
    {
        return buildString( 0 );
    }

    private void print( PrintStream s )
    {
	for( int i=0; i<indices.length; i++ ){
	    s.println( "" + indices[i] + " " + commonality[i] + " " + buildString( indices[i] ) );
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
		s.println( "maximum: " + indices[i] + " " + commonality[i] + " " + buildString( indices[i], commonality[i] ) );
	    }
	}
    }

    /**
     * Replaces the string at the given entry in the suffix array, and with
     * the given length, with the given code. Also updates part of the
     * administration, but does NOT leave the adminstration in sorted order.
     */
    private int replace( int pos, int len, short code )
    {
        int ix = indices[pos];

        // Move down all text behind the replaced string, and put in the
        // code.
        System.arraycopy( text, ix+1, text, ix+len, length-(ix+len) );
        text[ix] = code;

        // Now update the suffix array.
        for( int i=0; i<length; i++ ){
            int iy = indices[i];

            if( iy>ix ){
                // This position is affected by the replacement, ajust it.
                iy -= (len-1);
                if( iy<=ix ){
                    // This position is in the replaced string, mark it
                    // as obsolete.
                    iy = -1;
                }
                indices[i] = iy;
            }
        }

        // Now compact the indices and commonality arrays.
        int j = 0;      // Index of the next clean entry.
        for( int i=0; i<length; i++ ){
            if( indices[i]>= 0 ){
                // Copy this valid entry.
                // Note that before the first deleted entry is encountered,
                // we copy the entry onto itself. This is harmless.
                indices[j] = indices[i];
                commonality[j] =  commonality[i];
                if( i == pos ){
                    pos = j;
                }
                j++;
            }
        }

        length -= (len-1);

        // The suffix array is still not in the correct order, because
        // we haven't ajusted the length  of the commonality. However,
        // this will be corrected by the sorting pass we'll have to do anyway.
        return pos;
    }

    /** Given an entry in the suffix array, creates a new grammar rule
     * to take advantage of the commonality indicated by that entry.
     * It also covers any further entries with the same commonality.
     */
    private void applyCompression( int pos )
    {
        // First, move the grammar text aside.
        int len = commonality[pos];
        short t[] = new short[len];
        System.arraycopy( text, indices[pos], t, 0, len );

        // Now assign a new variable 
        short variable = nextcode++;
        pos = 1+replace( pos-1, len, variable );
        while( pos<length && commonality[pos] == len ){
            System.out.println( "Repeated replacement" );
            pos = replace( pos, len, variable );
            pos++;
        }

        // Add the new grammar rule in front by copying the grammar body
        // back into the text, and terminating it with the grammar code of
        // the new rule. The grammar rule can be recognized because it is the
        // first occurence of that particular nonterminal code.

        System.out.println( "text.length=" + text.length +  " len=" + len + " length=" + length );
        System.arraycopy( text, 0, text, len+1, length );
        System.arraycopy( t, 0, text, 0, len );
        text[len] = variable;

        for( int i=0; i<length; i++ ){
            indices[i] += len+1;
        }
        System.arraycopy( indices, 0, indices, len+1, length );
        for( int i=0; i<=len; i++ ){
            indices[i] = i;
        }

        length += len+1;
        sort();
    }

    public void test() throws VerificationException
    {
        // Verify that the elements are in fact ordered, and that the
        // commonality entry is correct.
        for( int i=1; i<indices.length; i++ ){
            if( !areCorrectlyOrdered( indices[i-1], indices[i] ) ){
                throw new VerificationException(
                    "suffix array order is incorrect between " + (i-1) + " and " + i + " (`" + buildString( indices[i-1] ) + "' and `" + buildString( indices[i] ) + "'"
                );
            }
        }
    }

    /** Apply one step in the folding process. */
    public void applyFolding()
    {
        if( length == 0 ){
            return;
        }
	int max = 0;
        int repeats = 0;

	for( int i=1; i<length; i++ ){
            int r = 0;

	    if( commonality[i]>=commonality[max] ){
                r = 2;
                for( int j = i+1; j<length; j++ ){
                    if( commonality[j] == commonality[i] ){
                        r++;
                    }
                    else {
                        break;
                    }
                }
            }
	    if( r>repeats || commonality[i]>commonality[max] ){
		max = i;
                repeats = r;
	    }
	}

        // Calculate the nett gain of replacing the maximal communality:
        // the gain of using references to the new rule, minus the cost of the
        // new rule.
        int gain = (repeats*(commonality[max]-1)) - (commonality[max]+1);
        if( traceCompressionCosts ){
            System.out.println( "String [" + buildString( indices[max], commonality[max] ) + "] has " + repeats + " repeats: gain=" + gain );
        }
        if( gain>0 ){
            // It is worthwile to do this compression.
            applyCompression( max );
        }
    }

    /** Returns a compressed version of the string represented by
     * this suffix array.
     */
    public ByteBuffer compress()
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
