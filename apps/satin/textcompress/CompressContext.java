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

    public int applyMove( byte text[], int pos, ByteBuffer out )
    {
        return pos;
    }

    public void registerRef( byte c, int pos )
    {
    }
}
