// File: $Id$

class ByteBuffer implements java.io.Serializable {
    byte buf[];
    int sz;

    ByteBuffer( int len )
    {
        buf = new byte[len];
        sz = 0;
    }

    ByteBuffer()
    {
        this( 1000 );
    }

    void append( byte b )
    {
        if( sz>=buf.length ){
            byte newbuf[] = new byte[sz+sz+1];
            System.arraycopy( buf, 0, newbuf, 0, sz );
            buf = newbuf;
        }
        buf[sz++] = b;
    }

    void append( short v )
    {
        append( (byte) (v>>8) );
        append( (byte) (v & 255) );
    }

    void replicate( int d, int len )
    {
        int pos = sz-d;
        if( pos<0 ){
            System.err.println( "Cannot replicate from beyond the start of the buffer: pos=" + pos );
            System.exit( 1 );
        }
        if( sz+len>buf.length ){
            byte newbuf[] = new byte[sz+len+10];
            System.arraycopy( buf, 0, newbuf, 0, sz );
            buf = newbuf;
        }
        System.arraycopy( buf, pos, buf, sz, len );
        sz += len;
    }

    public void appendRef( int pos, Backref ref )
    {
        int backpos = ref.backpos;
        int len = ref.len-Configuration.MINIMAL_SPAN;
        int d = (pos-backpos)-Configuration.MINIMAL_SPAN;
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
                append( Magic.BACKL1D1 );
                append( (byte) len );
                append( (byte) d );
            }
            else {
                append( Magic.BACKL2D1 );
                append( (short) len );
                append( (byte) d );
            }
        }
        else {
            if( len<256 ){
                append( Magic.BACKL1D2 );
                append( (byte) len );
                append( (short) d );
            }
            else {
                append( Magic.BACKL2D2 );
                append( (short) len );
                append( (short) d );
            }
        }
    }
}
