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

    public ByteBuffer compress( byte text[], int pos, ByteBuffer out, CompressContext ctx )
    {
        return out;
    }

    public ByteBuffer compress( byte text[] )
    {
        CompressContext ctx = new CompressContext( 
            Configuration.ALPHABET_SIZE,
            Configuration.BACKREFERENCES
        );
        ByteBuffer out = new ByteBuffer();
        out = compress( text, 0, out, ctx );
        sync();
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
