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

    private static short[] buildShortArray( byte text[] )
    {
        short arr[] = new short[text.length+1];

        for( int i=0; i<text.length; i++ ){
            arr[i] = (short) text[i];
        }
        arr[text.length] = STOP;
        return arr;
    }

    /** Returns the number of common characters in the two given spans. */
    private int commonLength( int i0, int i1 )
    {
	int n = 0;
	while( (text[i0] == text[i1]) && (text[i0] != STOP) ){
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
	if( text[i0+n] == STOP ){
	    // The sortest string is first, this is as it should be.
	    return true;
	}
	if( text[i1+n] == STOP ){
	    // The sortest string is last, this is not good.
	    return false;
	}
	return (text[i0+n]<text[i1+n]);
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
		if( areCorrectlyOrdered( i0+l, i1+l ) ){
		    // Things are sorted, or we're at the start of the array,
		    // take a step forward.
		    i++;
		}
		else {
		    // Things are in the wrong order, swap them and step back.
		    int tmp = indices[i];
		    indices[i] = indices[i-1];
		    i--;
		    indices[i] = tmp;
		}
	    }
	}
    }

    /** Builds the suffix array and the commonality array. */
    private void buildArray() throws VerificationException
    {
	indices = new int[length];
	commonality = new int[length];

	commonality[0] = -1;
	for( int i=0; i<indices.length; i++ ){
	    indices[i] = i;
	}

        sort();
    }

    private SuffixArray( short text[] ) throws VerificationException
    {
        this.text = text;

        buildArray();
    }

    SuffixArray( byte t[] ) throws VerificationException
    {
        length = t.length;
        text = buildShortArray( t );

        buildArray();
    }

    SuffixArray( String text ) throws VerificationException
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
            else if( c == STOP ){
                s += "<stop>";
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

    public void printGrammar()
    {
        int start = 0;
        boolean busy;
        int rule = 0;   // The rule we're printing, or 0 for the top level.

        for( int i=0; i<length+1; i++ ){
            if( i>=length || text[i] == STOP ){
                // We're at the end of a rule. Print it.
                String var;

                if( rule == 0 ){
                    var = "<start>";
                    rule = FIRSTCODE;
                }
                else {
                    var = "<" + rule + ">";
                    rule++;
                }
                System.out.println( var + " -> [" + buildString( start, i-start ) + "]" );
                start = i+1;
            }
        }
    }

    /**
     * Replaces the string at the given entry in the suffix array, and with
     * the given length, with the given code. Also updates part of the
     * administration, but does NOT leave the adminstration in sorted order.
     */
    private void replace( int pos, int len, short code, boolean deleted[] )
    {
        int ix = indices[pos];

        text[ix] = code;

        for( int i=1; i<len; i++ ){
            // TODO: make sure there is no overlap.
            deleted[ix+i] = true;
        }
    }

    /** Given an entry in the suffix array, creates a new grammar rule
     * to take advantage of the commonality indicated by that entry.
     * It also covers any further entries with the same commonality.
     */
    private void applyCompression( int pos ) throws VerificationException
    {
        // First, move the grammar text aside.
        int len = commonality[pos];
        short t[] = new short[len];
        System.arraycopy( text, indices[pos], t, 0, len );

        boolean deleted[] = new boolean[length];

        // Now assign a new variable and replace all occurences.
        short variable = nextcode++;

        replace( pos-1, len, variable, deleted );
        int i = pos;
        while( i<length && commonality[i] == len ){
            replace( i, len, variable, deleted );
            i++;
        }

        int j = 0;
        for( i=0; i<length; i++ ){
            if( !deleted[i] ){
                // Before the first deleted character this copies the
                // character onto itself. This is harmless.
                text[j++] = text[i];
            }
        }
        length = j;

        // Separate the previous stuff from the grammar rule that follows.
        text[length++] = STOP;

        // Add the new grammar rule.
        System.arraycopy( t, 0, text, length, len );
        length += len;
        text[length] = STOP;

        // Re-initialize the indices array, and sort it again.
        // TODO: do this in a more subtle manner.
        for( i=0; i<length; i++ ){
            indices[i] = i;
        }
        sort();
    }

    /**
     * Verify that `indices' is a permutation of the character positions.
     * This is done by (1) ensuring there are no repeats, and (2)
     * all indices are valid positions.
     */
    private void verifyIndicesIsPermutation()
    {
        boolean seen[] = new boolean[length];

        for( int i=0; i<length; i++ ){
            int ix = indices[i];

            if( ix<0 ){
                System.out.println( "Error: Negative index: indices[" + i + "]=" + ix );
            }
            else if( ix>=indices.length ){
                System.out.println( "Error: Index out of range: indices[" + i + "]=" + ix );
            }
            else if( seen[ix] ){
                System.out.println( "Error: Duplicate index: indices[" + i + "]=" + ix );
            }
            else {
                seen[ix] = true;
            }
        }
    }

    public void test() throws VerificationException
    {
        verifyIndicesIsPermutation();

        // Verify that the elements are in fact ordered, and that the
        // commonality entry is correct.
        for( int i=1; i<length; i++ ){
            int ix0 = i-1;
            int ix1 = i;

            if( !areCorrectlyOrdered( indices[ix0], indices[ix1] ) ){
                int l = commonLength( indices[ix0], indices[ix1] );
                short c0 = text[indices[ix0]+l];
                short c1 = text[indices[ix1]+l];

                throw new VerificationException(
                    "suffix array order is incorrect between " + ix0 + " and " + ix1 + " (`" + buildString( indices[ix0] ) + "' and `" + buildString( indices[ix1] ) + "'); common length is " + l + "; c0=" + c0 + "; c1=" + c1 + "; ordered=" + (c0<c1)
                );
            }
        }
    }

    /** Apply one step in the folding process. */
    public boolean applyFolding() throws VerificationException
    {
        if( length == 0 ){
            return false;
        }
	int max = 0;
        int maxgain = 0;
        int repeats = 0;

	for( int i=1; i<length; i++ ){
            int r = 0;
            int gain = -1;

	    if( commonality[i]>2 ){
                r = 2;
                for( int j = i+1; j<length; j++ ){
                    if( commonality[j] == commonality[i] ){
                        // TODO: make sure we don't look at these
                        // again in the next top-level iteration,
                        // since that will never be useful.
                        r++;
                    }
                    else {
                        break;
                    }
                }
                // We gain by replacing of `r' instances of
                // the string with one code, but we must add a new
                // grammar rule to the text.
                // TODO: take the cost of encoding these things into account.
                gain = (r*(commonality[i]-1)) - (commonality[i]+1);
            }
	    if( gain>maxgain ){
		max = i;
                repeats = r;
                maxgain = gain;
	    }
	}

        if( maxgain>0 ){
            // It is worthwile to do this compression.
            if( traceCompressionCosts ){
                System.out.println( "String [" + buildString( indices[max], commonality[max] ) + "] has " + repeats + " repeats: gain=" + maxgain );
            }
            applyCompression( max );
            if( doVerification ){
                test();
            }
        }
        return maxgain>0;
    }

    /** Returns a compressed version of the string represented by
     * this suffix array.
     */
    public ByteBuffer compress() throws VerificationException
    {
        boolean success;

        do {
            success = applyFolding();
            if( traceIntermediateGrammars && success ){
                printGrammar();
            }
        } while( success );
	return new ByteBuffer( text, length );
    }

    public static void main( String args[] )
    {
        try {
            SuffixArray t = new SuffixArray( args[0] );

            if( doVerification ){
                t.test();
            }
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
