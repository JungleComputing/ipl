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

    private SuffixArray( short text[] )
    {
        this.text = text;
        length = text.length;
    }

    public SuffixArray( ShortBuffer b )
    {
        this( b.getText() );
    }

    SuffixArray( byte t[] )
    {
        length = t.length;
        text = buildShortArray( t );
    }

    SuffixArray( String text )
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
            arr[i] = (short) ((int) text[i] & 0xFF);
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
        return (text[i0]<text[i1]);
    }

    /** Returns true iff i0 refers to a smaller text than i1. */
    private boolean areCorrectlyOrdered( int i0, int i1 )
    {
        int n = commonLength( i0, i1 );
        return isSmallerCharacter( i0+n, i1+n );
    }

    /** Returns true iff the indices are correctly ordered. */
    private boolean rightIndexOrder( int i0, int i1 )
    {
        if( i0>=length && i1>=length ){
            return i0>i1;       // Provide a stable sort.
        }
        if( i0>=length ){
            return true;
        }
        if( i1>=length ){
            return false;
        }
        if( text[i0]==text[i1] ){
            return i0>i1;
        }
        return text[i0]>text[i1];
    }

    private boolean rightIndexOrder1( int i0, int i1 )
    {
        boolean res = rightIndexOrder1( i0, i1 );
        System.out.println( "i0=" + i0 + " i1=" + i1 + " text[" + i0 + "]=" + text[i0] + " text[" + i1 + "]=" + text[i1] + " res=" + res );
        return res;
    }

    private void sort( int indices[], int start, int end, int offset )
    {
        // This implements Shell sort.
        // Unfortunately we cannot use the sorting functions from the library
        // (e.g. java.util.Arrays.sort), since the ones that work on int
        // arrays do not accept a comparison function, but only allow
        // sorting into natural order.
        int length = end - start;
        int jump = length;
        boolean done;

        while( jump>1 ){
            jump = (5*jump)/11;
            if( jump == 0 ){
                jump = 1;
            }

            do {
                done = true;

                for( int j = 0; j<(length-jump); j++ ){
                    int i = j + jump;
                    int ixi = indices[start+i];
                    int ixj = indices[start+j];

                    if( !rightIndexOrder( ixi+offset, ixj+offset ) ){
                        // Things are in the wrong order, swap them and step back.
                        indices[start+i] = ixj;
                        indices[start+j] = ixi;
                        if( !rightIndexOrder( ixj+offset, ixi+offset ) ){
                            System.out.println( "There is no right order for " + ixj + " and " + ixi );
                        }
                        done = false;
                    }
                }
            } while( !done );
        }
        if( false ){
            for( int i=start; i<end; i++ ){
                System.out.println( "i=" + i + " indices[" + i + "]=" + indices[i] + " offset=" + offset + " text[" + (indices[i]+offset) + "]=" + text[indices[i]+offset] );
            }
        }
    }


    /** Fills the given index and commonality arrays with groups
     * of elements sorted according to the character at the given offset.
     */
    private int sort1(
        int next[],
        int indices1[],
        boolean comm[],
        int p,
        int indices[],
        int start,
        int end,
        int offset
    )
    {
        if( end-start<1 ){
            // A span of 1 element is never interesting.
            return 0;
        }
        if( end-start == 2 ){
            // A span of 2 elements can only survive if the two
            // are the same.
            int i0 = indices[start]+offset;
            int i1 = indices[start+1]+offset;
            if( i0>=length || i1>=length || text[i0] != text[i1] ){
                return 0;
            }
            indices1[p] = indices[start];
            indices1[p+1] = indices[start+1];
            comm[p] = false;
            comm[p+1] = true;
            return p+2;
        }
        if( true ){
            //System.out.println( "Elements: " + (end-start) );
            int slot[] = new int[nextcode];

            java.util.Arrays.fill( slot, -1 );
            {
                // Fill the hash buckets
                // Walk from back to front to make sure the chains are in
                // the right order.
                int n = end;
                while( n>start ){
                    n--;
                    int i = indices[n];

                    if( i+offset<length ){
                        short ix = text[i+offset];

                        next[i] = slot[ix];
                        slot[ix] = i;
                    }
                }
            }

            slot[STOP] = -1;

            // Write out the indices and commonalities.
            for( int n=0; n<nextcode; n++ ){
                int i = slot[n];
                boolean c = false;

                if( i != -1 && next[i] != -1 ){
                    // There is a repeat, write out this chain.
                    while( i != -1 ){
                        indices1[p] = i;
                        i = next[i];
                        comm[p] = c;
                        c = true;
                        p++;
                    }
                }
            }
        }
        else {
            // First, sort all indices in this range
            sort( indices, start, end, offset );

            // And then copy out all interesting spans.
            int i = start;
            short prev = -1;
            int startp = p;
            while( i<end ){
                int ix = indices[i++];
                short c = text[ix];

                if( c != prev ){
                    if( startp>p && !comm[p-1] ){
                        p--;
                    }
                    comm[p] = false;
                }
                else {
                    comm[p] = true;
                }
                prev = c;
                indices1[p++] = ix;
            }
        }
        return p;
    }

    /** Determine whether the given list of indices is useful for the
        non-overlapping repeat we're searching. Too short or only overlapping
        repeats cause a reject.
    */
    private boolean isAcceptable( int indices[], int start, int end, int offset )
    {
        if( end-start<2 ){
            return false;
        }
        for( int ix=start; ix<end; ix++ ){
            for( int iy=ix+1; iy<end; iy++ ){
                if( (indices[ix]+offset)<=indices[iy] ){
                    return true;
                }
            }
        }
        return false;
    }


    class Result {
        int offset;
        int l;
        int indices[];
        boolean comm[];

        Result( int offset, int l, int indices[], boolean comm[] )
        {
            this.offset = offset;
            this.l = l;
            this.indices = indices;
            this.comm = comm;
        }

        void print()
        {
            for( int i=0; i<l; i++ ){
                System.out.println( "" + indices[i] + " [" + buildString( indices[i], offset ) + "]" + (comm[i]?"":" <-")  );
            }
        }
    }

    /** Returns a selection of the suffix array that allows easy extraction
     * of the longest repeats.
     */
    private Result selectRepeats( int top )
    {
        int next[] = new int[length];
        int indices[] = new int[length];
        boolean comm[] = new boolean[length];
        int offset = 0;

        for( int i=0; i<length; i++ ){
            indices[i] = i;
        }
        int l = sort1( next, indices, comm, 0, indices, 0, length, offset );
        for(;;){
            int indices1[] = new int[l];
            boolean comm1[] = new boolean[l];
            int ix = 0;
            offset++;
            boolean acceptable = false;
            int p = 0;
            int spans = 0;
            if( false ){
                Result r = new Result( offset, l, indices, comm );
                r.print();
            }
            while( ix<l ){
                int start = ix;
                int oldp = p;

                ix++;
                while( ix<l && comm[ix] ){
                    ix++;
                }
                p = sort1( next, indices1, comm1, p, indices, start, ix, offset );
                if( isAcceptable( indices1, oldp, p, offset ) ){
                    acceptable = true;
                    comm1[oldp] = false;
                    spans++;
                }
                else {
                    p = oldp;
                }
            }
            if( spans<=top ){
                // The next step would leave too few choices, stick to
                // the current one.
                break;
            }
            indices = indices1;
            comm = comm1;
            l = p;
        }
        return new Result( offset-1, l, indices, comm );
    }

    /** Sorts the given range. */
    private void sort( int indices[], int commonality[], int start, int end )
    {
        // This implements Shell sort.
        // Unfortunately we cannot use the sorting functions from the library
        // (e.g. java.util.Arrays.sort), since the ones that work on int
        // arrays do not accept a comparison function, but only allow
        // sorting into natural order.
        int jump = end-start;
        final int kC = 1;	// known commonality.

        while( jump>1 ){
            jump = (5*jump)/11;
            if( jump == 0 ){
                jump = 1;
            }
            boolean done;

            do {
                done = true;

                if( jump == 1 ){
                    // We treat the case for jump == 1 separately, because
                    // we want to fill in commonality at the same time.
                    final int s = end-1;

                    for( int j = start; j<s; j++ ){
                        int i = j + 1;
                        int ixi = indices[i];
                        int ixj = indices[j];

                        // We know the first kC characters are equal...
                        int n = kC+commonLength( ixi+kC, ixj+kC );
                        commonality[i] = n;
                        if( text[ixj+n]>text[ixi+n] ){
                            // Things are in the wrong order, swap them and step back.
                            indices[i] = ixj;
                            indices[j] = ixi;
                            done = false;
                        }
                    }
                }
                else {
                    final int s = (end-jump);

                    for( int j = start; j<s; j++ ){
                        int i = j + jump;
                        int ixi = indices[i];
                        int ixj = indices[j];

                        // We know the first kC characters are equal...
                        int n = kC+commonLength( ixi+kC, ixj+kC );
                        if( text[ixj+n]>text[ixi+n] ){
                            // Things are in the wrong order, swap them and step back.
                            indices[i] = ixj;
                            indices[j] = ixi;
                            done = false;
                        }
                    }
                }
            } while( !done );
        }

        commonality[start] = 0;
    }

    /* void sort(int a[], int lo0, int hi0) throws Exception { */
    private void qsort( int indices[], int start, int end )
    {
        int lo = start;
        int hi = end-1;
        final int kC = 1;	// known commonality.

        if (lo >= hi) {
            return;
        }
        if( start+2 == end ) {
            int ixi = indices[start];
            int ixj = indices[start+1];

            // We know the first kC characters are equal...
            int n = kC+commonLength( ixi+kC, ixj+kC );
            if( text[ixj+n]>text[ixi+n] ){
                indices[start] = ixj;
                indices[start+1] = ixi;
            }
            return;
        }


        /*
         *  Pick a pivot and move it out of the way
         */
        int mid = (lo+hi)/2;
        int pivot = indices[mid];
        indices[mid] = indices[hi];
        indices[hi] = pivot;

        while( lo < hi ) {
            /*
             *  Search forward from indices[lo] until an element is found that
             *  is greater than the pivot or lo >= hi 
             */
            while( lo < hi ){
                int ixi = indices[lo];
                int n = kC+commonLength( pivot+kC, ixi+kC );
                if( text[ixi+n]>text[pivot+n] ){
                    break;
                }
                lo++;
            }

            /*
             *  Search backward from indices[hi] until element is found that
             *  is less than the pivot, or lo >= hi
             */
            while ( lo < hi ) {
                int ixi = indices[hi];
                int n = kC+commonLength( pivot+kC, ixi+kC );
                if( text[pivot+n]>text[ixi+n] ){
                    break;
                }
                hi--;
            }

            /*
             *  Swap elements indices[lo] and indices[hi]
             */
            if( lo < hi ) {
                int T = indices[lo];
                indices[lo] = indices[hi];
                indices[hi] = T;
            }

        }

        /*
         *  Put the median in the "center" of the list
         */
        indices[end-1] = indices[hi];
        indices[hi] = pivot;

        /*
         *  Recursive calls, elements indices[start] to indices[lo-1] are less than or
         *  equal to pivot, elements indices[hi+1] to indices[hi0] are greater than
         *  pivot.
         */
        qsort( indices, start, lo );
        qsort( indices, hi+1, end );
    }

    private void newsort( int indices[], int commonality[], int start, int end )
    {
        qsort( indices, start, end );
        commonality[start] = 0;

        // TODO: see if there is a way to weave this into the qsort.
        for( int i=start+1; i<end; i++ ){
            commonality[i] = 1+ commonLength( indices[i-1]+1, indices[i]+1 );
        }
    }

    /** Sorts the administration arrays to implement ordering. */
    private void buildAdministrationUnused( int indices[], int commonality[] )
    {
        /**
         * This version uses pre-sorting in slots based on two characters.
         * In principle, that should be faster because more different sets
         * are created, but unfortunately, the cost of maintaining more
         * slots makes it slower than the standard one. Only a truely
         * cunning plan can save this version.
         */

        int slots[][] = new int[nextcode][];
        int next[] = new int[length];
        int filledSlots;

        {
            // First, construct the chains for the single character case.
            // We guarantee that the positions are in increasing order.


            // Fill each next array element with the next element with the
            // same character. We walk the string from back to front to
            // get the links in the correct order.
            int i = length;
            while( i>0 ){
                i--;
                int ix = text[i];
                int iy = text[i+1];

                if( ix != STOP && iy != STOP ){
                    if( slots[ix] == null ){
                        int n[] = new int[nextcode];
                        Arrays.fill( n, -1 );
                        slots[ix] = n;
                    }
                    int s[] = slots[ix];

                    next[i] = s[iy];
                    s[iy] = i;
                }
            }
        }

        // Now copy out the slots into the indices array.
        int ix = 0;	// Next entry in the indices array.

        for( int i=0; i<slots.length; i++ ){
            if( slots[i] != null ){
                int s[] = slots[i];

                for( int j=0; j<s.length; j++ ){
                    int p = s[j];
                    int start = ix;

                    while( p != -1 ){
                        indices[ix++] = p;
                        p = next[p];
                    }
                    if( start+1<ix ){
                        sort( indices, commonality, start, ix );
                    }
                    else {
                        // A single entry is not interesting, remove it
                        // from the list of indices.
                        ix = start;
                    }
                }
            }
        }

        // Fill all unused slots (due to removed entries) with
        // uninteresting information.
        while( ix<length ){
            commonality[ix] = 0;
            ix++;
        }
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

            Arrays.fill( slots, -1 );

            // Fill each next array element with the next element with the
            // same character. We walk the string from back to front to
            // get the links in the correct order.
            int i = length;
            while( i>0 ){
                i--;
                int ix = text[i];

                if( ix != STOP ){
                    next[i] = slots[ix];
                    slots[ix] = i;
                }
            }
            filledSlots = slots.length;
        }

        // Now copy out the slots into the indices array.
        int ix = 0;	// Next entry in the indices array.

        for( int i=0; i<filledSlots; i++ ){
            int p = slots[i];
            int start = ix;

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

            if( c == '\n' ){
                s.append( "<nl>" );
            }
            else if( c == '\r' ){
                s.append( "<cr>" );
            }
            else if( c == '\t' ){
                s.append( "<tab>" );
            }
            else if( c<255 ){
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
    public void applyCompression( Step s )
    {
        final int oldLength = length;   // Remember the old length for verif.

        if( traceCompressionSteps ){
            System.out.println( "Applying compression step " + s );
        }

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
        StepList res;

        if( true ){
            int p = 0;
            int candidates[] = new int[length];
            int mincom = MINCOMMONALITY;

            /** The indices in increasing alphabetical order of their suffix. */
            int indices[] = new int[length];

            /** For each position in `indices', the number of elements it
             * has in common with the previous entry, or -1 for element 0.
             * The commonality may be overlapping, and that should be taken
             * into account by compression algorithms.
             */
            int commonality[] = new int[length];

            buildAdministration( indices, commonality );
            res = new StepList( top );
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
                            //mincom = Math.max( mincom, len-1 );
                            res.add( new Step( candidates, p, len ) );
                            break;
                        }
                        len--;
                    }
                }
            }
        }
        else {
            Result reps = selectRepeats( top );
            // reps.print();

            boolean commonality[] = reps.comm;
            int indices[] = reps.indices;
            int l = reps.l;
            int offset = reps.offset;
            int candidates[] = new int[l];

            res = new StepList( top );
            int ix = 0;
            while( ix<l ){
                int start = ix;
                candidates[0] = indices[ix];
                int p = 1;
                int minsz = length;

                ix++;
                while( ix<l && commonality[ix] ){
                    int posj = indices[ix];

                    int sz = disjunctMatch( candidates[p-1], posj );
                    if( sz>=offset ){
                        candidates[p++] = posj;
                        if( sz<minsz ){
                            minsz = sz;
                        }
                    }
                    ix++;
                }
                if( p>1 && minsz>=MINCOMMONALITY ){
                    Step s = new Step( candidates, p, minsz );
                    res.add( s );
                }
            }
        }
        return res;
    }

    public int getLength() { return length; }
    
    public ByteBuffer getByteBuffer() { return new ByteBuffer( text, length ); }
}
