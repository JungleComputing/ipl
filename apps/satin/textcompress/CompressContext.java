// File: $Id$

/**
 * The context of the compression.
 */

class CompressContext implements java.io.Serializable {
    /**
     * For each hash code of the current position, a list of backreferences
     * to previous occurences of that string.
     */
    int backref[][];

    public CompressContext( int alsz, int backrefs )
    {
        backref = new int[alsz][backrefs];
        for( int i=0; i<alsz; i++ ){
            int refs[] = backref[i];

            for( int j=0; j<backrefs; j++ ){
                refs[j] = -1;
            }
        }
    }

    public CompressContext( int arr[][] )
    {
        backref = arr;
    }

    public Object clone()
    {
        int arr[][] = new int[backref.length][];

        for( int i=0; i<backref.length; i++ ){
            arr[i] = (int []) backref[i].clone();
        }
        return new CompressContext( arr );
    }

    public void outputRef( byte text[], int pos, Backref ref, ByteBuffer out )
    {
        int backpos = ref.backpos;
        int len = ref.len-Configuration.MINIMAL_SPAN;
        int d = (pos-backpos)-Configuration.MINIMAL_SPAN;
        System.out.println( "Encoding reference " + ref + " from position " + pos );
        if( len<0 ){
            System.err.println( "bad match encoding attempt: len=" + len );
            System.exit( 1 );
        }
        if( d<0 ){
            System.err.println( "bad match encoding attempt: d=" + d );
            System.exit( 1 );
        }
        if( d<256 ){
            if( len<256 ){
                out.append( Magic.BACKL1D1 );
                out.append( (byte) len );
                out.append( (byte) d );
            }
            else {
                out.append( Magic.BACKL1D1 );
                out.append( (short) len );
                out.append( (byte) d );
            }
        }
        else {
            if( len<256 ){
                out.append( Magic.BACKL1D2 );
                out.append( (byte) len );
                out.append( (short) d );
            }
            else {
                out.append( Magic.BACKL1D2 );
                out.append( (short) len );
                out.append( (short) d );
            }
        }
    }

    /**
     * Given a hash value 'c' and a position 'pos', registers the fact
     * that a string entry with this hash code is at the given position.
     */
    public void registerRef( int c, int pos )
    {
        int refs[] = backref[c];

        int i = refs.length;
        while( i>1 ){
            i--;
            refs[i] = refs[i-1];
        }
        refs[0] = pos;
    }
}
