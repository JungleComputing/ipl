// File: $Id$

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

class Compress extends ibis.satin.SatinObject implements CompressorInterface
{
    static final boolean traceMatches = true;

    public static byte[] readFile( File f )
        throws java.io.IOException
    {
        int sz = (int) f.length();

        byte buf[] = new byte[sz];

        FileInputStream s = new FileInputStream( f );
        int n = s.read( buf );
        if( n != sz ){
            System.err.println( "File is " + sz + " bytes, but I could only read " + n + " bytes. I give up." );
            System.exit( 1 );
        }
        return buf;
    }

    // Given a compression move to make, predict the gain of this move.
    public int evaluateMove( byte text[], int pos, CompressContext ctx, int move, int depth )
    {
        int scores[] = new int[Configuration.BACKREFERENCES];
        int forwardGain = 0;
        int localGain = 0;
        int fwdPos = pos;

        int backpos = ctx.backref[(int) text[pos]][move];
        if( backpos>=0 ){
            int matchSize = Helpers.matchSpans( text, backpos, pos );

            if( matchSize>=Configuration.MINIMAL_SPAN ){
                localGain = matchSize-Helpers.refEncodingSize( pos-backpos, matchSize );
            }
        }
        ctx.registerRef( text[pos], pos );
        if( depth<Configuration.LOOKAHEAD_DEPTH && pos+Configuration.MINIMAL_SPAN<text.length ){
            for( int mv = 0; move<Configuration.BACKREFERENCES; move++ ){
                CompressContext c1 = (CompressContext) ctx.clone();
                scores[move] = evaluateMove( text, pos, c1, mv, depth+1 );
            }
            sync();
            int bestMove = -1;
            int bestGain = 0;
            for( int mv = 0; move<Configuration.BACKREFERENCES; move++ ){
                if( scores[move] > bestGain ){
                    bestGain = scores[move];
                    bestMove = mv;
                }
            }
            forwardGain = bestGain;
        }
        return localGain + forwardGain;
    }

    public void selectBestMove( Backref mv, byte text[], int pos, CompressContext ctx )
    {
        int scores[] = new int[Configuration.BACKREFERENCES];
        int bestGain = 0;
        int bestMove = 0;
        CompressContext cs = (CompressContext) ctx.clone();
        Backref plainMove = new Backref();
        selectBestMove( plainMove, text, pos+1, cs );
        int hashcode = (int) text[pos];

        for( int m = 0; m<Configuration.BACKREFERENCES; m++ ){
            CompressContext c1 = (CompressContext) ctx.clone();
            scores[m] = evaluateMove( text, pos, c1, m, 0 );
        }
        sync();
        for( int m = 0; m<Configuration.BACKREFERENCES; m++ ){
            if( scores[m] > bestGain ){
                bestGain = scores[m];
                bestMove = m;
            }
        }
        if( bestGain>0 ){
            mv.backpos = ctx.backref[hashcode][bestMove];
            // TODO: fill in length.
            mv.gain = bestGain;
        }
        else {
            mv.backpos = -1;
            mv.len = -1;
            mv.gain = plainMove.gain;
        }
    }

    public ByteBuffer compress( byte text[] )
    {
        CompressContext ctx = new CompressContext( 
            Configuration.ALPHABET_SIZE,
            Configuration.BACKREFERENCES
        );
        int pos = 0;
        ByteBuffer out = new ByteBuffer();
        while( pos+Configuration.MINIMAL_SPAN<text.length ){
            Backref mv = new Backref();
            int m = 0;
            final int hashcode = (int) text[pos];

            mv.backpos = -1;
            final int backpos = ctx.backref[hashcode][m];
            if( backpos>=0 && backpos<pos-Configuration.MINIMAL_SPAN ){
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
        byte text[] = readFile( infile );
	long startTime = System.currentTimeMillis();

        Compress c = new Compress();

        ByteBuffer buf = c.compress( text );
        FileOutputStream output = new FileOutputStream( outfile );
        output.write( buf.buf, 0, buf.sz );
        output.close();

	long endTime = System.currentTimeMillis();
	double time = ((double) (endTime - startTime))/1000.0;

	System.out.println( "ExecutionTime: " + time );
        System.out.println( "In: " + text.length + " bytes, out: " + buf.sz + " bytes." );
    }
}
