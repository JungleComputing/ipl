// File: $Id$

class ByteBuffer implements java.io.Serializable, Magic {
    byte buf[];
    int sz;

    public ByteBuffer( int len )
    {
        buf = new byte[len];
        sz = 0;
    }

    public ByteBuffer()
    {
        this( 1000 );
    }

    public ByteBuffer( short text[] )
    {
	this( text.length );
	append( text );
    }

    /** Encode the given array of shorts as a byte array. For compactness
     * an encoding with variable length is used, where the bytes have the
     * following meaning:
     * byte value   followup bytes    meaning
     * 0..127        0                The byte itself is the value
     * ESCAPE1       1                The followup is the value
     * ESCAPE2       2                The followup is the value (MSB first)
     * 128..ESCAPE2  1                Byte itself -128 is MSB, next is LSB.
     */
    public void append( short text[] )
    {
	// Allocate a buffer at the size that is minimally necessary.
        short arr[] = new short[text.length+1];

        for( int i=0; i<text.length; i++ ){
	    int v = (text[i] & 0xFFFF);
	    if( v<128 ){
		append( (byte) v );
	    }
	    else if( v<ESCAPE2 ){
		append( (byte) ESCAPE1 );
		append( (byte) v );
	    }
	    else if( v<(128<<8) ){
		append( (byte) (128+(v>>8)) );
		append( (byte) (v & 255) );
	    }
	    else {
		append( (byte) ESCAPE2 );
		append( (short) v );
	    }
        }
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

    public int getSize() { return sz; }

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
}
