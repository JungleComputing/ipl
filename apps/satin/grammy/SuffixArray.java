// File: $Id$

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;

public class SuffixArray implements Configuration, Magic, java.io.Serializable {
    /** The lowest interesting commonality. */
    private static final int MINCOMMONALITY = 3;

    /** The buffer containing the compressed text. */
    private short text[];

    /** The number of elements in `text' that are relevant. */
    private int length;

    /** The next grammar rule number to hand out. */
    private short nextcode = FIRSTCODE;

    private SuffixArray( short text[] ) throws VerificationException
    {
        this.text = text;
        length = text.length;
    }

    public SuffixArray( ShortBuffer b ) throws VerificationException
    {
        this( b.getText() );
    }

    SuffixArray( byte t[] ) throws VerificationException
    {
        length = t.length;
        text = buildShortArray( t );
    }

    SuffixArray( String text ) throws VerificationException
    {
        this( text.getBytes() );
    }

    private SuffixArray( short t[], int l, short nextcode )
    {
        text = t;
        length = l;
        this.nextcode = nextcode;
    }

    /**
     * Returns a clone of the SuffixArray.
     * @return The clone.
     */
    public Object clone()
    {
        short nt[] = new short[length];
        System.arraycopy( text, 0, nt, 0, length );

        return new SuffixArray( nt, length, nextcode );
    }

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
	while( (text[i0+n] == text[i1+n]) && (text[i0+n] != STOP) ){
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

    /** Returns true iff i0 refers to a smaller character than i1. */
    private boolean isSmallerCharacter( int i0, int i1 )
    {
	if( text[i0] == STOP ){
	    // The shortest string is first, this is as it should be.
	    return true;
	}
	if( text[i1] == STOP ){
	    // The shortest string is last, this is not good.
	    return false;
	}
	return (text[i0]<text[i1]);
    }

    /** Returns true iff i0 refers to a smaller text than i1. */
    private boolean areCorrectlyOrdered( int i0, int i1 )
    {
        int n = commonLength( i0, i1 );
	return isSmallerCharacter( i0+n, i1+n );
    }

    /** Sorts the given range. */
    private void sort( int indices[], int commonality[], int start, int end )
    {
        // This implements Shell sort.
        // Unfortunately we cannot use the sorting functions from the library
        // (e.g. java.util.Arrays.sort), since the ones that work on int
        // arrays do not accept a comparison function, but only allow
        // sorting into natural order.
	int length = end - start;
        int jump = length;
        boolean done;
	final int kC = 1;	// known commonality.

        while( jump>1 ){
            jump /= 2;

            do {
                done = true;

		if( jump == 1 ){
		    for( int j = 0; j<(length-1); j++ ){
			int i = j + 1;
			int ixi = indices[start+i];
			int ixj = indices[start+j];

			// We know the first character is equal...
			int n = kC+commonLength( ixi+kC, ixj+kC );
			commonality[start+i] = n;
			if( !isSmallerCharacter( ixj+n, ixi+n ) ){
			    // Things are in the wrong order, swap them and step back.
			    indices[start+i] = ixj;
			    indices[start+j] = ixi;
			    done = false;
			}
		    }
		}
		else {
		    for( int j = 0; j<(length-jump); j++ ){
			int i = j + jump;
			int ixi = indices[start+i];
			int ixj = indices[start+j];

			int n = kC+commonLength( ixi+kC, ixj+kC );
			if( !isSmallerCharacter( ixj+n, ixi+n ) ){
			    // Things are in the wrong order, swap them and step back.
			    indices[start+i] = ixj;
			    indices[start+j] = ixi;
			    done = false;
			}
		    }
		}
            } while( !done );
        }

	commonality[start] = 0;
    }

    /** Sorts the administration arrays to implement ordering. */
    private void buildAdministration( int indices[], int commonality[] )
    {
	int slots[] = new int[nextcode];
	int next[] = new int[length];
        int filledSlots;

        {
            // First, construct the chains for the single character case.
            // We guarantee that the positions are in increasing order.
            int prev[] = new int[nextcode];
            java.util.Arrays.fill( slots, -1 );
            java.util.Arrays.fill( prev, -1 );

            // Fill each next array element with the next element with the
            // same character.
            for( int i=0; i<length; i++ ){
                int ix = text[i];

                if( prev[ix] == -1 ){
                    slots[ix] = i;
                }
                else {
                    next[prev[ix]] = i;
                }
                prev[ix] = i;
            }
            for( int ix=0; ix<slots.length; ix++ ){
                if( prev[ix] != -1 ){
                    next[prev[ix]] = -1;
                }
            }
            filledSlots = slots.length;
        }

        if( false ){
            int newslots[] = new int[slots.length*slots.length];
            int newnext[] = new int[length];
            int p = 0;
            int step = 1;
            for( int i=0; i<slots.length; i++ ){

                if( slots[i] == -1 ){
                    // This slot is empty. Next!
                    continue;
                }
                for( int j=0; j<slots.length; j++ ){
                    int ixi = slots[i];
                    int ixj = slots[j];
                    int previ = -1;
                    int n = 0;

    toploop:        while( ixj != -1 ){
                        while( ixi+step<ixj ){
                            ixi = next[ixi];
                            if( ixi == -1 ){
                                break toploop;
                            }
                        }
                        if( ixi+step == ixj ){
                            // We have a combination.
                            if( previ == -1 ){
                                newslots[p] = ixi;
                            }
                            else {
                                newnext[previ] = ixi;
                            }
                            previ = ixi;
                            newnext[ixi] = -1;
                            n++;
                        }
                        ixj = next[ixj];
                    }
                    if( n>1 ){
                        // This is an interesting repeat, we'll keep it.
                        p++;
                    }
                }
            }
            if( false ){
                System.out.println( "Found " + p + " interesting combinations of length " + (2*step) + "." );
            }
            filledSlots = p;
            slots = newslots;
            next = newnext;
        }

	// Now copy out the slots into the indices array.
	int ix = 0;	// Next entry in the indices array.

	for( int i=0; i<filledSlots; i++ ){
	    int p = slots[i];
	    int start = ix;

	    if( i == STOP ){
		// The hash slot for the STOP symbol is not interesting.
		continue;
	    }
	    while( p != -1 ){
		indices[ix++] = p;
		p = next[p];
	    }
	    if( start+1<ix ){
		sort( indices, commonality, start, ix );
	    }
	    else {
                // A single entry is not interesting, skip it.
		ix = start;
	    }
	}

	// Fill all unused slots with uninteresting information.
	while( ix<length ){
	    commonality[ix] = 0;
	    ix++;
	}
    }

    String buildString( int start, int len )
    {
        StringBuffer s = new StringBuffer( len+8 );
	int i = start;

        while( len>0 && i<this.length  ){
            int c = (text[i] & 0xFFFF);

            if( c<255 ){
                s.append( (char) c );
            }
            else if( c == STOP ){
                s.append( "<stop>" );
            }
            else {
                s.append( "<" + c + ">" );
            }
	    i++;
	    len--;
        }
        return new String( s );
    }

    String buildString( int start )
    {
	return buildString( start, length );
    }

    String buildString()
    {
        return buildString( 0 );
    }

    String buildString( Step s )
    {
        return buildString( s.occurences[0], s.len );
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
    public void applyCompression( Step s ) throws VerificationException
    {
        final int oldLength = length;   // Remember the old length for verif.

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

        if( doVerification ){
            int gain = s.getGain();

            if( length+gain != oldLength ){
                System.out.println( "Error: predicted gain was " + gain + ", but realized gain is " + (oldLength-length) );
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
    public StepList selectBestSteps( int top )
    {
        int mincom = MINCOMMONALITY;
        int candidates[] = new int[length];
        int p = 0;

	/** The indices in increasing alphabetical order of their suffix. */
	int indices[] = new int[length];

	/** For each position in `indices', the number of elements it
	 * has in common with the previous entry, or -1 for element 0.
	 * The commonality may be overlapping, and that should be taken
	 * into account by compression algorithms.
	 */
	int commonality[] = new int[length];

        buildAdministration( indices, commonality );


        StepList res = new StepList( top );
        for( int i=1; i<length; i++ ){
            if( commonality[i]>=mincom ){
                // A new candidate match. Start a list, and see if we
                // get at least two non-overlapping strings.
                int pos0 = indices[i-1];
                candidates[0] = pos0;
                p = 1;

                int len = commonality[i];

                while( len>mincom ){
		    // Now search for non-overlapping substrings that 
		    // are equal for `len' characters. All possibilities
		    // are the subsequent entries in the suffix array, up to
                    // the first one with less than 'len' characters
                    // commonality.
		    // We must test each one for overlap with all
		    // previously selected strings.
                    // TODO: this fairly arbitrary way of gathering candidates
                    // may not be optimal: a different subset of strings may
                    // be larger.
		    int j = i;
		    while( j<length && commonality[j]>=len ){
			int posj = indices[j];

			if( !areOverlapping( candidates, p, posj, len ) ){
			    candidates[p++] = posj;
			}
			j++;
		    }

                    if( p>1 ){
                        // HEURISTIC: anything shorter than this is probably
                        // not a reasonable candidate for best compression step.
                        // (But we could be wrong.)
                        // mincom = Math.max( mincom, len-1 );
                        res.add( new Step( candidates, p, len ) );
                    }
                    len--;
		}
            }
        }
        return res;
    }

    public int getLength() { return length; }
    
    public ByteBuffer getByteBuffer() { return new ByteBuffer( text, length ); }
}
