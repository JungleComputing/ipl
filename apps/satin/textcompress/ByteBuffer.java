// File: $Id$

public final class ByteBuffer implements java.io.Serializable {
    private byte buf[];
    private int sz;

    public ByteBuffer( int len )
    {
        buf = new byte[len];
        sz = 0;
    }

    public ByteBuffer()
    {
        this( 1000 );
    }

    /** Returns a byte array containing the current text in the buffer. */
    public byte [] getText()
    {
        byte res[] = new byte[sz];

        System.arraycopy( buf, 0, res, 0, sz );
        return res;
    }

    /** Returns a string containing the current text in the buffer. */
    public String toString()
    {
	return new String( buf, 0, sz );
    }

    public void append( byte b )
    {
        if( sz>=buf.length ){
            byte newbuf[] = new byte[sz+sz+1];
            System.arraycopy( buf, 0, newbuf, 0, sz );
            buf = newbuf;
        }
        buf[sz++] = b;
    }

    public void append( short v )
    {
        append( (byte) (v>>8) );
        append( (byte) (v & 255) );
    }

    public int getLength() { return sz; }

    public void replicate( int d, int len )
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

    public void write( java.io.OutputStream s )
        throws java.io.IOException
    {
        s.write( buf, 0, sz );
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
