// File: $Id$

/**
 * The context of the compression.
 */

class CompressContext implements java.io.Serializable {
    /**
     * For each hash code, the foremost position that matches this
     * hash code.
     */
    int heads[];
    int backrefs[];

    public CompressContext( int alsz, int textsize )
    {
        heads = new int[alsz];
        backrefs = new int[textsize];

        for( int i=0; i<alsz; i++ ){
            heads[i] = -1;
        }
    }

    private CompressContext( int heads[], int backrefs[] )
    {
        this.heads = heads;
        this.backrefs = backrefs;
    }

    public Object clone()
    {
        return new CompressContext( (int []) heads, (int []) backrefs );
    }

    public static void outputRef( byte text[], int pos, Backref ref, ByteBuffer out )
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
                out.append( Magic.BACKL2D1 );
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
                out.append( Magic.BACKL2D2 );
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
        backrefs[pos] = heads[c];
        heads[c] = pos;
    }
}
