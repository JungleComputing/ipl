// File: $Id$

import java.io.File;

class Decompress {
    /** Private constructor to prevent instantiation of this class. */
    private Decompress() {}

    /** Given a byte buffer, return a decompressed one. */
    public static ByteBuffer decompress( ByteBuffer buf )
    {
        ShortBuffer s = buf.decodeByteStream();
        s.decompress();
	return new ByteBuffer( s.getText() );
    }

    public static ByteBuffer decompress( byte buf[] )
    {
        return decompress( new ByteBuffer( buf ) );
    }

    /**
     * Allows execution of the class.
     * @param args The command-line arguments.
     */
    public static void main( String args[] ) throws java.io.IOException
    {
	if( args.length != 2 ){
	    System.err.println( "Usage: <compressed-text> <decompressed-text>" );
	    System.exit( 1 );
	}
	File infile = new File( args[0] );
	File outfile = new File( args[1] );
        byte text[] = Helpers.readFile( infile );
	long startTime = System.currentTimeMillis();

        ByteBuffer buf = decompress( text );

        Helpers.writeFile( outfile, buf );

	long endTime = System.currentTimeMillis();
	double time = ((double) (endTime - startTime))/1000.0;

	System.out.println( "ExecutionTime: " + time );
        System.out.println( "In: " + text.length + " bytes, out: " + buf.getLength() + " bytes." );
    }
}
