// File: $Id$

import java.io.File;

class Compress extends ibis.satin.SatinObject
{
    static final boolean traceMatches = false;
    static final boolean traceLookahead = false;

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

    private static int[] collectBackrefs( byte text[], int backrefs[], int pos )
    {
        // First count the number of backreferences.
        int n = 0;

        int backpos = backrefs[pos];
        while( backpos>=0 ){
            if( backpos<pos-Configuration.MINIMAL_SPAN ){
                if( text[backpos+1] == text[pos+1] && text[backpos+2] == text[pos+2] ){
                    // This is a sensible backref.
                    n++;
                }
            }
            backpos = backrefs[backpos];
        }

        // And now build an array with them.
        int res[] = new int[n];
        backpos = backrefs[pos];
        n = 0;
        while( backpos>=0 ){
            if( backpos<pos-Configuration.MINIMAL_SPAN ){
                if( text[backpos+1] == text[pos+1] && text[backpos+2] == text[pos+2] ){
                    res[n++] = backpos;
                }
            }
            backpos = backrefs[backpos];
        }
        return res;
    }

    public Backref shallowEvaluateBackref( final byte text[], final int backrefs[], int backpos, int pos )
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

    public Backref selectBestMove( byte text[], int backrefs[], int pos, int depth )
    {
        Backref mv = Backref.buildCopyBackref( pos );
        boolean haveAlternatives = false;

        if( pos+Configuration.MINIMAL_SPAN>=text.length ){
            return mv;
        }
        int sites[] = collectBackrefs( text, backrefs, pos );

        Backref results[] = new Backref[Magic.MAX_COST+1];
        for( int i=0; i<sites.length; i++ ){
            Backref r = shallowEvaluateBackref( text, backrefs, sites[i], pos );
            if( r != null ){
                int cost = r.getCost();

                if( results[cost] == null || results[cost].len<r.len ){
                    results[cost] = r;
                    haveAlternatives = true;
                }
            }
        }

        if( !haveAlternatives && depth == 0 ){
            // The only possible move is a copy, so don't bother to evaluate
            // it any further.
            if( traceLookahead ){
                System.out.println( "At pos=" + pos + " only a copy is possible" );
            }
            return mv;
        }

        if( depth<Configuration.LOOKAHEAD_DEPTH ){
            // Evaluate the gain of just copying the character.
            Backref mv1 = selectBestMove( text, backrefs, pos+1, depth+1 );
	    // See if we can get rid of this sync.
	    sync();
            mv.addGain( mv1 );
        }

        if( traceLookahead ){
            for( int i=0; i<depth; i++ ){
                System.out.print( ' ' );
            }
            System.out.println( "D" + depth + ": considering move: " + mv );
            for( int c=0; c<results.length; c++ ){
                Backref r = results[c];
                if( r != null ){
                    for( int i=0; i<depth; i++ ){
                        System.out.print( ' ' );
                    }
                    System.out.println( "D" + depth + ": considering move: " + r );
                }
            }
        }

        if( depth<Configuration.LOOKAHEAD_DEPTH ){
            // We have some recursion left, add the best gain from the recursion
            // to our own score.
            for( int i=0; i<results.length; i++ ){
                Backref r = results[i];

                if( r != null ){
                    Backref m = selectBestMove( text, backrefs, pos+r.len, depth+1 );
                    sync();
                    r.addGain( m );
                }
            }
            sync();
        }

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
        if( traceLookahead ){
            for( int i=0; i<depth; i++ ){
                System.out.print( ' ' );
            }
            System.out.println( "D" + depth + ": best move is: " + mv );
        }
        return mv;
    }

    public ByteBuffer compress( byte text[] )
    {
        int backrefs[] = buildBackrefs( text );
        int pos = 0;
        ByteBuffer out = new ByteBuffer();

        while( pos<Configuration.MINIMAL_SPAN && pos<text.length ){
            out.append( text[pos++] );
        }
        while( pos+Configuration.MINIMAL_SPAN<text.length ){
            Backref mv = selectBestMove( text, backrefs, pos, 0 );
            sync();
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

    /**
     * Allows execution of the class.
     * @param args The command-line arguments.
     */
    public static void main( String args[] ) throws java.io.IOException
    {
	if( args.length != 2 ){
	    System.err.println( "Usage: <text> <compressedtext>" );
	    System.exit( 1 );
	}
	File infile = new File( args[0] );
	File outfile = new File( args[1] );
        byte text[] = Helpers.readFile( infile );
	long startTime = System.currentTimeMillis();

        Compress c = new Compress();

        ByteBuffer buf = c.compress( text );

        Helpers.writeFile( outfile, buf );

	long endTime = System.currentTimeMillis();
	double time = ((double) (endTime - startTime))/1000.0;

	System.out.println( "ExecutionTime: " + time );
        System.out.println( "In: " + text.length + " bytes, out: " + buf.sz + " bytes." );
    }
}
