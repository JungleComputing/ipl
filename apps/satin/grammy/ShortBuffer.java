// File: $Id$

class ShortBuffer implements java.io.Serializable, Magic {
    short buf[];
    private int sz;

    public ShortBuffer( int len )
    {
        buf = new short[len];
        sz = 0;
    }

    public ShortBuffer()
    {
        this( 1000 );
    }

    public ShortBuffer( short text[], int len )
    {
        this( len );
	append( text, len );
    }

    public ShortBuffer( short text[] )
    {
	this( text, text.length );
    }

    /** Returns a byte array containing the current text in the buffer. */
    public byte [] getText()
    {
        byte res[] = new byte[sz];

        System.arraycopy( buf, 0, res, 0, sz );
        return res;
    }

    /** Ensures that the buffer has room for at least newsz elements. */
    private void reserve( int newsz )
    {
        if( newsz>buf.length ){
            int d = newsz - buf.length;
            if( d<3 ){
                newsz += sz;
            }
            short newbuf[] = new short[newsz];
            System.arraycopy( buf, 0, newbuf, 0, sz );
            buf = newbuf;
        }
    }

    public void append( short b )
    {
        reserve( sz+1 );
        buf[sz++] = b;
    }

    public void append( short b[], int len )
    {
        reserve( sz+len );
        System.arraycopy( b, 0, buf, sz, len );
        sz += len;
    }

    public void append( short text[] )
    {
        append( text, text.length );
    }

    public int getLength() { return sz; }
}
