// File: $Id$

import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;

class Helpers {
    /**
     * Given a span size, returns the number of bytes required to
     * encode it.
     */
    static int spanEncodingSize( int n )
    {
        if( n<Configuration.MINIMAL_SPAN ){
            return -1;
        }
        n -= Configuration.MINIMAL_SPAN;
        if( n<256 ){
            return 1;
        }
        // Note that if a span is too large, we truncate it, so there
        // is always something to encode.
        return 2;
    }

    /**
     * Given a match distance, returns the number of bytes required to
     * encode it.
     */
    static int distanceEncodingSize( int n )
    {
        if( n<Configuration.MINIMAL_SPAN ){
            return -1;
        }
        n -= Configuration.MINIMAL_SPAN;
        if( n<256 ){
            return 1;
        }
        if( n<65536 ){
            return 2;
        }
        return -1;
    }
    
    /**
     * Given a match distance and length, return the number of bytes
     * required to encode it.
     */
    static int refEncodingSize( int d, int sz )
    {
        int dsz = distanceEncodingSize( d );
        int szsz = spanEncodingSize( sz );
        if( dsz<0 || szsz<0 ){
            return -1;
        }
        return 1+dsz+szsz;
    }

    /**
     * Given a text and two positions, return the number of bytes
     * over which the two spans have the same text. Make sure
     * that the second, most forward span, is not touched by the
     * first span. Never return a match size that is longer than
     * (2^16-1)+MINIMAL_SPAN, since we won't be able to use it anyway.
     * @param arr The text array.
     * @param p1 The first position in the text.
     * @param p2 The second position in the text.
     * @return The number of equal bytes at these two positions.
     */
    static int matchSpans( byte arr[], int p1, int p2 )
    {
        int maxsz = Math.min( arr.length-p2, 65535+Configuration.MINIMAL_SPAN );

        if( p1>=p2 ){
            System.err.println( "Bad match in matchSpans()" );
            System.exit( 1 );
        }
        if( p1+maxsz>p2 ){
            maxsz = p2-p1;
        }
        for( int i=0; i<maxsz; i++ ){
            if( arr[p1+i] != arr[p2+i] ){
                return i;
            }
        }
        return maxsz;
    }

    static int decodeByte( byte b )
    {
        int v = b & 0xff;
        return v;
    }

    static int decodeShort( byte high, byte low )
    {
        return (decodeByte( high ) << 8) + decodeByte( low );
    }

    public static byte[] readFile( File f )
        throws java.io.IOException
    {
        final int sz = (int) f.length();

        byte buf[] = new byte[sz];

        FileInputStream s = new FileInputStream( f );
        int n = s.read( buf );
        if( n != sz ){
            System.err.println( "File is " + sz + " bytes, but I could only read " + n + " bytes. I give up." );
            System.exit( 1 );
        }
        return buf;
    }

    public static void writeFile( File f, ByteBuffer buf )
        throws java.io.IOException
    {
        FileOutputStream output = new FileOutputStream( f );
        output.write( buf.buf, 0, buf.sz );
        output.close();
    }
}
