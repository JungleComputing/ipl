// File: $Id$

import java.io.File;

class Compress extends ibis.satin.SatinObject
{
    static final boolean traceMatches = false;

    public ByteBuffer compress( byte text[] )
    {
        CompressContext ctx = new CompressContext( 
            Configuration.ALPHABET_SIZE,
            text.length
        );
        int pos = 0;
        ByteBuffer out = new ByteBuffer();
        while( pos+Configuration.MINIMAL_SPAN<text.length ){
            Backref mv = new Backref();
            int m = 0;
            final int hashcode = (int) text[pos];

            mv.backpos = -1;
            int backpos = ctx.heads[hashcode];

            while( backpos>=0 ){
                if( backpos<pos-Configuration.MINIMAL_SPAN ){
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
                backpos = ctx.backrefs[backpos];
            }
            // TODO: calculate the gain of just copying the character.
            if( mv.backpos<0 ){
                // There is no backreference that gives any gain, so
                // just copy the character.
                ctx.registerRef( text[pos], pos );
                out.append( text[pos++] );
            }
            else {
                // There is a backreference that helps.
                ctx.outputRef( text, pos, mv, out );
                int endpos = pos+mv.len;
                while( pos<endpos ){
                    ctx.registerRef( (int) text[pos], pos );
                    pos++;
                }
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
