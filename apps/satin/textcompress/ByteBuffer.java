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
        System.err.println( "Replicate: backpos=" + pos + " sz=" + sz + " d=" + d + " len=" + len );
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
}
