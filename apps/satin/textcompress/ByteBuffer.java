// File: $Id$

class ByteBuffer implements java.io.Serializable {
    byte buf[];
    int sz;

    ByteBuffer()
    {
        buf = new byte[1000];
        sz = 0;
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
}
