// File: $Id$

import java.io.File;

class Compress extends ibis.satin.SatinObject
{
    static final boolean traceMatches = false;

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
                // This is a sensible backref.
                // TODO: We could verify that at least the minimal span
                // is equal.
                n++;
            }
            backpos = backrefs[backpos];
        }
        int res[] = new int[n];
        backpos = backrefs[pos];
        n = 0;
        while( backpos>=0 ){
            if( backpos<pos-Configuration.MINIMAL_SPAN ){
                res[n++] = backpos;
            }
            backpos = backrefs[backpos];
        }
        return res;
    }

    public Backref evaluateBackref( byte text[], int backrefs[], int backpos, int pos, int depth )
    {
        Backref r = new Backref();

        r.backpos = backpos;
        r.len = Helpers.matchSpans( text, backpos, pos );

        if( r.len>=Configuration.MINIMAL_SPAN ){
            r.gain = r.len-Helpers.refEncodingSize( pos-backpos, r.len );
            if( traceMatches ){
                System.out.println( "A match " + r + " at " + pos );
            }
        }
        return r;
    }

    public Backref selectBestMove( byte text[], int backrefs[], int pos, int depth )
    {
        // We always have the choice to just copy the character.
        Backref mv = new Backref();

        if( pos+Configuration.MINIMAL_SPAN>=text.length ){
            return mv;
        }
        int sites[] = collectBackrefs( text, backrefs, pos );
        if( sites.length>0 ){
            // If we have more choices, evaluate them ...
            Backref results[] = new Backref[sites.length];
            for( int i=0; i<sites.length; i++ ){
                results[i] = evaluateBackref( text, backrefs, sites[i], pos, depth );
            }
            sync();

            // .. and pick the best one.
            for( int i=0; i<results.length; i++ ){
                Backref r = results[i];

                if( r.gain>mv.gain ){
                    mv = r;
                }
            }
        }
        return mv;
    }

    public ByteBuffer compress( byte text[] )
    {
        int backrefs[] = buildBackrefs( text );
        int pos = 0;
        ByteBuffer out = new ByteBuffer();
        while( pos+Configuration.MINIMAL_SPAN<text.length ){
            Backref mv = selectBestMove( text, backrefs, pos, 0 );
            // TODO: calculate the gain of just copying the character.
            if( mv.backpos<0 ){
                // There is no backreference that gives any gain, so
                // just copy the character.
                out.append( text[pos++] );
            }
            else {
                // There is a backreference that helps.
                out.outputRef( pos, mv );
                pos += mv.len;
            }
        }
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
