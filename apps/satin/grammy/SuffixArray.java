// File: $Id$

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;

public class SuffixArray implements Configuration, Magic {
    /** The buffer containing the compressed text. */
    private short text[];

    /** The lowest interesting commonality. */
    private final int MINCOMMONALITY = 3;

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

    /** Given two text positions, calculates the non-overlapping
     * match of these two.
     */
    private int disjunctMatch( int ix0, int ix1 )
    {
	int n = 0;
        if( ix0 == ix1 ){
            // TODO: complain, this should never happen.
            return 0;
        }

        if( ix0>ix1 ){
            int h = ix0;
            ix0 = ix1;
            ix1 = h;
        }
	while( (ix0+n)<ix1 && (text[ix0+n] == text[ix1+n]) && (text[ix0+n] != STOP) ){
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
        // This implements Shell sort.
        // Unfortunately we cannot use the sorting functions from the library
        // (e.g. java.util.Arrays.sort), since the ones that work on int
        // arrays do not accept a comparison function, but only allow
        // sorting into natural order.
        int jump = length;
        boolean done;

        while( jump>1 ){
            jump /= 2;

            do {
                done = true;

                for( int j = 0; j<(length-jump); j++ ){
                    int i = j + jump;

                    if( !areCorrectlyOrdered( indices[j], indices[i] ) ){
                        // Things are in the wrong order, swap them and step back.
                        int tmp = indices[i];
                        indices[i] = indices[j];
                        indices[j] = tmp;
                        done = false;
                    }
                }
            } while( !done );
        }

        // TODO: integrate this with the stuff above.
        for( int i=1; i<length; i++ ){
            commonality[i] = commonLength( indices[i-1], indices[i] );
        }
        commonality[0] = -1;
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
     * Replaces the string at the given posititon, and with
     * the given length, with the given code. Also marks the replaced
     * text as deleted.
     */
    private void replace( int ix, int len, short code, boolean deleted[] )
    {
        text[ix] = code;

        Arrays.fill( deleted, ix+1, ix+len, true );
    }

    /** Given an entry in the suffix array, creates a new grammar rule
     * to take advantage of the commonality indicated by that entry.
     * It also covers any further entries with the same commonality.
     */
    private void applyCompression( Step s ) throws VerificationException
    {
        // First, move the grammar text aside.
        int len = s.len;
        short t[] = new short[len];
        System.arraycopy( text, s.occurences[0], t, 0, len );

        boolean deleted[] = new boolean[length];

        // Now assign a new variable and replace all occurences.
        short variable = nextcode++;

        int a[] = s.occurences;

        for( int i=0; i<a.length; i++ ){
            replace( a[i], len, variable, deleted );
        }

        int j = 0;
        for( int i=0; i<length; i++ ){
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
        for( int i=0; i<length; i++ ){
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

    /** Returns true if the strings with the given positions and length
     * overlap.
     * @return True iff the strings overlap.
     */
    private static boolean areOverlapping( int ix0, int ix1, int len )
    {
        if( ix0<ix1 ){
            return ix0+len>ix1;
        }
        else {
            return ix1+len>ix0;
        }
    }

    /** Returns true iff none of the `sz' strings in `a' overlaps with
     * `pos' over a length `len'.
     */
    private static boolean areOverlapping( int a[], int sz, int pos, int len )
    {
	for( int i=0; i<sz; i++ ){
	    if( areOverlapping( a[i], pos, len ) ){
		return true;
	    }
	}
	return false;
    }

    /**
     * Calculates the best folding step to take.
     * @return The best step, or null if there is nothing worthwile.
     */
    private Step selectBestStep()
    {
        int maxlen = 0;
        int mincom = MINCOMMONALITY;
        int candidates[] = new int[length];
        int p = 0;

        for( int i=1; i<length; i++ ){
            if( commonality[i]>=mincom ){
		// TODO: we should also consider pairs that
		// have too much overlap, but make it up in subsequent
		// entries. This does mean that we must create a new
		// candidate array for each new possibility, since we
		// may have to throw it away.
                int pos0 = indices[i-1];
		int pos1 = indices[i];
		int len = Math.min( Math.abs( pos1-pos0 ), commonality[i] );

		if( len>maxlen ){
		    // This is a better candidate.
		    candidates[0] = pos0;
		    candidates[1] = pos1;
		    p = 2;

		    // Now search for non-overlapping substrings that 
		    // are equal for `len' characters. All possibilities
		    // are the subsequent entries in the suffix array, up to
                    // the first one with less than 'len' characters
                    // commonality.
		    // We must test each one for overlap with all
		    // previously selected strings.
                    // TODO: this fairly arbitrary way of gathering candidates
                    // is not optimal, since a different subset of strings may
                    // be larger.
		    int j = i+1;
		    while( j<length && commonality[j]>=len ){
			int posj = indices[j];

			if( !areOverlapping( candidates, p, posj, len ) ){
			    candidates[p++] = posj;
			}
			j++;
		    }
		    maxlen = len;
		    mincom = len;
		}
            }
        }
        if( maxlen == 0 ){
            return null;
        }
        return new Step( candidates, p, maxlen );
    }

    /**
     * Applies one step in the folding process.
     * @return True iff there was a useful compression step.
     */
    public boolean applyFolding() throws VerificationException
    {
        if( length == 0 ){
            return false;
        }
        Step mv = selectBestStep();

        if( mv != null && mv.getGain()>0 ){
            // It is worthwile to do this compression.
            if( traceCompressionCosts ){
                System.out.println( "Best step: string [" + buildString( mv.occurences[0], mv.len ) + "]: " + mv );
            }
            applyCompression( mv );
            if( doVerification ){
                test();
            }
            return true;
        }
        return false;
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
