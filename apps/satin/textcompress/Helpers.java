// File: $Id$

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
     * first span.
     */
    static int matchSpans( byte arr[], int p1, int p2 )
    {
        int maxsz = Math.max( arr.length-p2, 65535+Configuration.MINIMAL_SPAN );

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
}
