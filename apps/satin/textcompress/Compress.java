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

    public ByteBuffer compress( byte text[] )
    {
        int backrefs[] = buildBackrefs( text );
        int pos = 0;
        ByteBuffer out = new ByteBuffer();
        while( pos+Configuration.MINIMAL_SPAN<text.length ){
            Backref mv = new Backref();
            int m = 0;
            final int hashcode = (int) text[pos];

            mv.backpos = -1;

            int sites[] = collectBackrefs( text, backrefs, pos );
            for( int i=0; i<sites.length; i++ ){
                int backpos = sites[i];
                int matchSize = Helpers.matchSpans( text, backpos, pos );

                if( matchSize>=Configuration.MINIMAL_SPAN ){
                    if( traceMatches ){
                        System.out.println( "A match of " + matchSize + " bytes at positions " + backpos + " and " + pos );
                    }
                    int gain = matchSize-Helpers.refEncodingSize( pos-backpos, matchSize );
                    if( gain>0 ){
                        mv.backpos = backpos;
                        mv.len = matchSize;
                        mv.gain = gain;
                    }
                }
            }
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
