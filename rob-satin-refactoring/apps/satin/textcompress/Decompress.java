// File: $Id$

import java.io.File;

class Decompress
{
    /** Private constructor to prevent instantiation of this class. */
    private Decompress() {}

    public static ByteBuffer decompress( byte text[] )
    {
        int pos = 0;
        ByteBuffer out = new ByteBuffer( text.length );
        while( pos<text.length ){
            byte c = text[pos++];

            if( c == Magic.BACKL1D1 ){
                int len = Helpers.decodeByte( text[pos++] )+Configuration.MINIMAL_SPAN;
                int d = Helpers.decodeByte( text[pos++] )+Configuration.MINIMAL_SPAN;
                out.replicate( d, len );
            }
            else if( c == Magic.BACKL2D1 ){
                int len = Helpers.decodeShort( text[pos++], text[pos++] )+Configuration.MINIMAL_SPAN;
                int d = Helpers.decodeByte( text[pos++] )+Configuration.MINIMAL_SPAN;
                out.replicate( d, len );
            }
            else if( c == Magic.BACKL1D2 ){
                int len = Helpers.decodeByte( text[pos++] )+Configuration.MINIMAL_SPAN;
                int d = Helpers.decodeShort( text[pos++], text[pos++] )+Configuration.MINIMAL_SPAN;
                out.replicate( d, len );
            }
            else if( c == Magic.BACKL2D2 ){
                int len = Helpers.decodeShort( text[pos++], text[pos++] )+Configuration.MINIMAL_SPAN;
                int d = Helpers.decodeShort( text[pos++], text[pos++] )+Configuration.MINIMAL_SPAN;
                out.replicate( d, len );
            }
            else {
                out.append( c );
            }
        }
        return out;
    }

    public static ByteBuffer decompress( ByteBuffer buf )
    {
        return decompress( buf.getText() );
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

        ByteBuffer buf = decompress( text );

        Helpers.writeFile( outfile, buf );

        long endTime = System.currentTimeMillis();
        double time = ((double) (endTime - startTime))/1000.0;

        System.out.println( "ExecutionTime: " + time );
        System.out.println( "In: " + text.length + " bytes, out: " + buf.getLength() + " bytes." );
    }
}
