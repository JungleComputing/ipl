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

    public int applyMove( byte text[], int pos, int mv, ByteBuffer out )
    {
        int backpos = backref[(int) text[pos]][mv];
        int len = Helpers.matchSpans( text, backpos, pos )-Configuration.MINIMAL_SPAN;
        int d = (pos-backpos)-Configuration.MINIMAL_SPAN;
        if( len<0 || d<0 ){
            System.err.println( "bad match encoding attempt" );
            System.exit( 1 );
        }
        if( d<256 ){
            if( len<256 ){
                out.append( Magic.BACK1B1B );
                out.append( (byte) len );
                out.append( (byte) d );
            }
            else {
            }
        }
        else {
        }
        return pos;
    }

    /**
     * Given a hash value 'c' and a position 'pos', registers the fact
     * that a string entry with this hash code is at the given position.
     */
    public void registerRef( byte c, int pos )
    {
        int refs[] = backref[(int) c];

        int i = refs.length;
        while( i>1 ){
            i--;
            refs[i] = refs[i-1];
        }
        refs[0] = pos;
    }
}
