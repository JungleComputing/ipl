// File: $Id$

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

class Compress extends ibis.satin.SatinObject implements CompressorInterface
{
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
    public int lookahead( byte text[], int pos, CompressContext ctx, int move, int depth )
    {
        int scores[] = new int[Configuration.BACKREFERENCES];
        int forwardGain = 0;
        int localGain = 0;
        int fwdPos = pos;

        ctx.registerRef( text[pos], pos );
        if( depth<Configuration.LOOKAHEAD_DEPTH && pos+Configuration.MINIMAL_SPAN<text.length ){
            for( int mv = 0; move<Configuration.BACKREFERENCES; move++ ){
                CompressContext c1 = (CompressContext) ctx.clone();
                scores[move] = lookahead( text, pos, c1, mv, depth+1 );
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

    public ByteBuffer compress( byte text[] )
    {
        CompressContext ctx = new CompressContext( 
            Configuration.ALPHABET_SIZE,
            Configuration.BACKREFERENCES
        );
        int pos = 0;
        ByteBuffer out = new ByteBuffer();
        while( pos+Configuration.MINIMAL_SPAN<text.length ){
            int scores[] = new int[Configuration.BACKREFERENCES];
            int bestGain = 0;
            int bestMove = 0;

            for( int mv = 0; mv<Configuration.BACKREFERENCES; mv++ ){
                CompressContext c1 = (CompressContext) ctx.clone();
                scores[mv] = lookahead( text, pos, c1, mv, 0 );
            }
            sync();
            for( int mv = 0; mv<Configuration.BACKREFERENCES; mv++ ){
                if( scores[mv] > bestGain ){
                    bestGain = scores[mv];
                    bestMove = mv;
                }
            }
            // TODO: calculate the gain of just copying the character.
            if( bestGain<=0 ){
                // There is no backreference that gives any gain, so
                // just copy the character.
                out.append( text[pos++] );
            }
            else {
                pos = ctx.applyMove( text, pos, out );
                // There is a backreference that helps.
            }
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
    }
}
