// File: $Id$

import java.io.File;

class Compress extends ibis.satin.SatinObject implements CompressorInterface
{
    static final boolean traceMatches = false;
    static final boolean traceLookahead = false;
    static final boolean traceSiteCount = false;
    static final boolean parallelizeShallowEvaluation = false;
    static int lookahead_depth = Configuration.LOOKAHEAD_DEPTH;
    static boolean doVerification = false;
    static boolean quiet = false;

    private static void generateIndent( java.io.PrintStream str, int n )
    {
        for( int i=0; i<n; i++ ){
            str.print( ' ' );
        }
    }

    // Given a byte array, build an array of backreferences. That is,
    // construct an array that at each text position gives the index
    // of the previous occurence of that hash code, or -1 if there is none.
    private int[] buildBackrefs( byte text[] )
    {
        int heads[] = new int[Configuration.ALPHABET_SIZE];
        int backrefs[] = new int[text.length];

        for( int i=0; i<Configuration.ALPHABET_SIZE; i++ ){
            heads[i] = -1;
        }
        for( int i=0; i<text.length; i++ ){
            int hashcode = (int) text[i];
            backrefs[i] = heads[hashcode];
            heads[hashcode] = i;
        }
        return backrefs;
    }

    /**
     * Returns true iff the given text matches over MINIMAL_SPAN at the
     * two given positions. No explicit bounds checking or overlap
     * checking is done.
     */
    private static boolean matchesMinSpan( byte text[], int p1, int p2 )
    {
        return text[p1] == text[p2] &&
            text[p1+1] == text[p2+1] &&
            text[p1+2] == text[p2+2] &&
            text[p1+3] == text[p2+3];
    }

    /**
     * Returns a list of positions in the text that contain a possible
     * text match for our current text.
     * @param text The text to compress.
     * @param backrefs For each position, a link to the first previous similar text.
     * @param pos The position for which we want possible 
     * @return A list of indices of similar text fragments.
     */
    private static int[] collectBackrefs( byte text[], int backrefs[], int pos )
    {
        // First count the number of backreferences.
        int n = 0;

        int backpos = backrefs[pos];
        while( backpos>=0 ){
            if( backpos<pos-Configuration.MINIMAL_SPAN ){
                if( matchesMinSpan( text, backpos, pos ) ){
                    // This is a sensible backref.
                    n++;
                }
            }
            backpos = backrefs[backpos];
        }

        // And now build an array with them. The conditions
        // of this loop are identical to the first loop, but now we fill
        // an array.
        int res[] = new int[n];
        backpos = backrefs[pos];
        n = 0;
        while( backpos>=0 ){
            if( backpos<pos-Configuration.MINIMAL_SPAN ){
                if( matchesMinSpan( text, backpos, pos ) ){
                    res[n++] = backpos;
                }
            }
            backpos = backrefs[backpos];
        }
        return res;
    }

    /**
     * Given a list of backreferences and a match length, return an
     * adapted version of the nearest (and hence cheapest) backreference that
     * covers this match length.
     * @param results The list of backreferences to choose from.
     * @param n The length of backreference to build.
     * @return The constructed backreference.
     */
    Backref buildBestMove( Backref results[] , int n )
    {
        Backref r = null;

        for( int i=0; i<results.length; i++ ){
            Backref b = results[i];

            if( b != null && b.len>=n ){
                // This backreference can cover our need for a span of
                // at least n bytes.
                if( r == null || r.backpos<b.backpos ){
                    // This is the first backreference, or this one is
                    // closer to our current position.
                    r = b;
                }
            }
        }
        return new Backref( r.backpos, r.pos, n );
    }

    /**
     * Given a text, a back position and a current position, return
     * a backreference for this back position. The backreference contains
     * a match length that is indirectly used to evaluate this backreference.
     */
    public Backref shallowEvaluateBackref( final byte text[], int backpos, int pos )
    {
        Backref r;

        int len = Helpers.matchSpans( text, backpos, pos );
        if( len >= Configuration.MINIMAL_SPAN ){
            r = new Backref( backpos, pos, len );

            if( traceMatches ){
                System.out.println( "A match " + r );
            }
        }
        else {
            r = null;
        }
        return r;
    }

    /** The same as shallowEvaluateBackref(), but declared to be a Satin
     * job.
     */
    public Backref shallowEvaluateBackrefJob( final byte text[], int backpos, int pos )
    {
        return shallowEvaluateBackref( text, backpos, pos );
    }



    /**
     * @param text The text to compress.
     * @param backrefs The index of the first previous occurence of this hash.
     * @param pos The position to select the move for.
     * @param bestpos First uncompressed byte by the best move known to parent.
     * @param max_shortening The maximum shortening we consider for a backreference.
     * @param depth The recursion depth of this selection.
     * @param max_depth The maximal recursion depth.
     * @return The best move, or null if we can't better bestpos.
     */
    public Backref selectBestMove( byte text[], int backrefs[], int pos, int bestpos, int max_shortening, int depth, int max_depth )
    {
        Backref mv = null;
        boolean haveAlternatives = false;
        int maxLen = 0;
        int minLen = text.length;

        if( pos+1>=bestpos ){
            // A simple character copy is worth considering.
            mv = Backref.buildCopyBackref( pos );
        }
        if( pos+Configuration.MINIMAL_SPAN>=text.length ){
            // There are too few characters left for a worthwile backref,
            // or there are no previous text spans.
            // We may as well stop.
            return mv;
        }
        if( traceLookahead ){
            generateIndent( System.out, depth );
            System.out.println( "D" +  depth + ": @" + pos + ": selecting best move improving on @" + bestpos );
        }

        Backref results[] = null;

        if( backrefs[pos] != -1 ){
            int sites[] = collectBackrefs( text, backrefs, pos );

            if( sites.length != 0 ){
                results = new Backref[Magic.MAX_COST+1];
                Backref a[] = new Backref[sites.length];

                if( traceSiteCount ){
                    System.out.println( "D" +  depth + ": @" + pos + ": there are " + sites.length + " sites" );
                }
                if( parallelizeShallowEvaluation ){
                    for( int i=0; i<sites.length; i++ ){
                        a[i] = shallowEvaluateBackrefJob( text, sites[i], pos );
                    }
                    sync();
                }
                else {
                    for( int i=0; i<sites.length; i++ ){
                        a[i] = shallowEvaluateBackref( text, sites[i], pos );
                    }
                }
                for( int i=0; i<sites.length; i++ ){
                    Backref r = a[i];

                    if( r != null ){
                        int cost = r.getCost();

                        // If this backreference is worth the extra trouble,
                        // register it.
                        if( pos+r.len>bestpos-cost ){
                            // This backreference is long enough that it may
                            // help beat the best move of the parent selectBestMove.
                            if(
                                results[cost] == null ||
                                results[cost].len<r.len ||
                                (results[cost].len==r.len && r.backpos>results[cost].backpos)
                            ){
                                // This backreferences is longer than the
                                // previous one we had registered for this cost.
                                results[cost] = r;
                                haveAlternatives = true;
                                if( maxLen<r.len ){
                                    maxLen = r.len;
                                }
                                int minl = 1+cost;
                                if( minl<minLen ){
                                    minLen = minl;
                                }
                            }
                        }
                    }
                }
            }
        }

        if( !haveAlternatives ){
            // The only possible move is a copy.
            if( mv != null && depth>0 && depth<max_depth ){
                // It is permitted and useful to evaluate the copy move
                // using recursion, so that higher levels can accurately
                // compare it to other alternatives.
                // Evaluate the gain of just copying the character.
                Backref mv1 = selectBestMove( text, backrefs, pos+1, pos+1, max_shortening, depth+1, max_depth );
                mv.addGain( mv1 );
            }
            if( traceLookahead ){
                generateIndent( System.out, depth );
                System.out.println( "D" + depth + ": no backrefs, so only move is: " + mv );
            }
            return mv;
        }

        if( traceLookahead ){
            if( mv != null ){
                generateIndent( System.out, depth );
                System.out.println( "D" + depth + ":  considering move " + mv );
            }
            if( results != null ){
                for( int c=0; c<results.length; c++ ){
                    Backref r = results[c];
                    if( r != null ){
                        generateIndent( System.out, depth );
                        System.out.println( "D" + depth + ":  considering move " + r );
                    }
                }
            }
        }

        if( depth<max_depth ){
            Backref mv1 = null;

            // We have some recursion depth left. We know we can backreference
            // a span of at most maxLen characters. In recursion see if it
            // is worthwile to shorten this to allow a longer subsequent
            // match.
            if( minLen<Configuration.MINIMAL_SPAN ){
                minLen = Configuration.MINIMAL_SPAN;
            }
            if( minLen<maxLen-max_shortening ){
                minLen = maxLen-max_shortening;
            }
            if( pos+minLen<=bestpos ){
                minLen = 1+bestpos-pos;
            }

            Backref a[] = new Backref[maxLen+1];
            if( traceLookahead ){
                generateIndent( System.out, depth );
                System.out.println( "D" + depth + ": @" + pos + ": evaluating backreferences of " + minLen + "..." + maxLen + " bytes" );
            }

            // Spawn recurrent processes to evaluate a character copy
            // and backreferences of a range of lengths.
            for( int i=minLen; i<=maxLen; i++ ){
                a[i] = selectBestMoveJob( text, backrefs, pos+i, pos+maxLen, max_shortening, depth+1, max_depth );
            }
            if( mv != null ){
                mv1 = selectBestMoveJob( text, backrefs, pos+1, pos+1, max_shortening, depth+1, max_depth );
            }
            sync();
            int bestGain = -1;
            if( mv != null && mv1 != null ){
                bestGain = mv1.getGain();
                mv.addGain( bestGain );
            }
            for( int i=minLen; i<=maxLen; i++ ){
                Backref r = a[i];

                if( r != null ){
                    Backref mymv = buildBestMove( results, i );
                    mymv.addGain( r );
                    int g = mymv.getGain();

                    if(
                        g>bestGain ||
                        (g == bestGain && mymv.backpos>mv.backpos)
                    ){
                        mv = mymv;
                        bestGain = g;
                    }
                }
            }
        }
        else {
            // We're at the end of recursion. Simply pick the match
            // with the best gain.

            if( results != null ){
                int bestGain = 0;

                for( int i=0; i<results.length; i++ ){
                    Backref r = results[i];

                    if( r != null ){
                        int g = r.getGain();

                        if( g>bestGain ){
                            mv = r;
                            bestGain = g;
                        }
                    }
                }
            }
        }
        if( traceLookahead ){
            generateIndent( System.out, depth );
            System.out.println( "D" + depth + ": best move is: " + mv );
        }
        return mv;
    }

    public Backref selectBestMoveJob( byte text[], int backrefs[], int pos, int bestpos, int max_shortening, int depth, int max_depth )
    {
        return selectBestMove( text, backrefs, pos, bestpos, max_shortening, depth, max_depth );
    }

    public ByteBuffer compress( byte text[], int max_shortening, int max_depth )
    {
        int backrefs[] = buildBackrefs( text );
        int pos = 0;
        ByteBuffer out = new ByteBuffer();

        while( pos<text.length && (pos<Configuration.MINIMAL_SPAN || backrefs[pos] == -1) ){
            out.append( text[pos++] );
        }
        while( pos+Configuration.MINIMAL_SPAN<text.length ){
            Backref mv = selectBestMove( text, backrefs, pos, pos, max_shortening, 0, max_depth );
            if( mv.backpos<0 ){
                // There is no backreference that gives any gain, so
                // just copy the character.
                out.append( text[pos++] );
            }
            else {
                // There is a backreference that helps, write it to
                // the output stream.
                out.appendRef( pos, mv );

                // And skip all the characters that we've backreferenced.
                pos += mv.len;
            }
        }

        // Write the last few characters without trying to compress.
        while( pos<text.length ){
            out.append( text[pos++] );
        }
        return out;
    }

    static void usage()
    {
        System.err.println( "Usage: [-quiet] [-verify] [-short <n>] [-depth <n>] <text-file> <compressed-file>" );
        System.err.println( "   or: [-quiet] [-verify] [-short <n>] [-depth <n>] -string <text-string> <compressed-file>" );
    }

    /**
     * Allows execution of the class.
     * @param args The command-line arguments.
     */
    public static void main( String args[] ) throws java.io.IOException
    {
        File infile = null;
        File outfile = null;
        int max_shortening = Configuration.MAX_SHORTENING;
        String intext = null;

        for( int i=0; i<args.length; i++ ){
            if( args[i].equals( "-verify" ) ){
                doVerification = true;
            }
            else if( args[i].equals( "-quiet" ) ){
                quiet = true;
            }
            else if( args[i].equals( "-short" ) ){
                i++;
                max_shortening = Integer.parseInt( args[i] );
            }
            else if( args[i].equals( "-depth" ) ){
                i++;
                lookahead_depth = Integer.parseInt( args[i] );
            }
            else if( args[i].equals( "-string" ) ){
                i++;
                if( intext != null || infile != null ){
                    System.err.println( "More than one text to compress given" );
                    usage();
                    System.exit( 1 );
                }
                intext = args[i];
            }
            else if( infile == null ){
                infile = new File( args[i] );
            }
            else if( outfile == null ){
                outfile = new File( args[i] );
            }
            else {
                System.err.println( "Superfluous parameter `" + args[i] + "'" );
                usage();
                System.exit( 1 );
            }
        }
        if( intext == null && (infile == null || outfile == null) ){
            usage();
            System.exit( 1 );
        }
        byte text[];
        if( infile != null ){
            text = Helpers.readFile( infile );
        }
        else if( intext != null ){
            text = intext.getBytes();
        }
        else {
            System.err.println( "No text to compress" );
            usage();
            System.exit( 1 );
            text = null;
        }
        long startTime = System.currentTimeMillis();

        if( !quiet ){
            System.out.println( "Recursion depth: " + lookahead_depth + ", max. shortening: " + max_shortening  );
        }

        Compress c = new Compress();

        ByteBuffer buf = c.compress( text, max_shortening, lookahead_depth );

        if( outfile != null ){
            Helpers.writeFile( outfile, buf );
        }

        long endTime = System.currentTimeMillis();
        double time = ((double) (endTime - startTime))/1000.0;

        if( !quiet ){
            System.out.println( "ExecutionTime: " + time );
            System.out.println( "In: " + text.length + " bytes, out: " + buf.getLength() + " bytes." );
        }
        if( doVerification ){
            ByteBuffer debuf = Decompress.decompress( buf );
            byte nt[] = debuf.getText();

            if( nt.length != text.length ){
                System.out.println( "Error: decompressed text has different length from original. Original is " + text.length + " bytes, decompression is " + nt.length + " bytes" );
                System.exit( 1 );
            }
            for( int i=0; i<nt.length; i++ ){
                if( nt[i] != text[i] ){
                    System.out.println( "Error: decompressed text differs from original at position " + i );
                    System.exit( 1 );
                }
            }
        }
    }
}
