// File: $Id$

public final class ByteBuffer implements java.io.Serializable, Magic {
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

    public ByteBuffer( short text[], int len )
    {
        this( len );
        append( text, len );
    }

    public ByteBuffer( short text[] )
    {
        this( text, text.length );
    }

    public ByteBuffer( byte txt[] )
    {
        this( txt.length );
        System.arraycopy( txt, 0, buf, 0, buf.length );
        sz = buf.length;
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
    public void append( short text[], int len )
    {
        // Allocate a buffer at the size that is minimally necessary.
        short arr[] = new short[text.length+1];

        for( int i=0; i<len; i++ ){
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

    public void append( short text[] )
    {
        append( text, text.length );
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

    public ShortBuffer decodeByteStream()
    {
        // Under-estimate the expected size of the resulting buffer as
        // half the byte buffer.
        ShortBuffer res = new ShortBuffer( sz/2 );

        for( int i=0; i<sz; i++ ){
            short v = (short) Helpers.decodeByte( buf[i] );

            if( v<128 ){
                res.append( v );
            }
            else if( v == ESCAPE1 ){
                i++;
                res.append( Helpers.decodeByte( buf[i] ) );
            }
            else if( v == ESCAPE2 ){
                i++;
                res.append( Helpers.decodeShort( buf[i], buf[i+1] ) );
                i++;
            }
            else {
                res.append( (Helpers.decodeShort( buf[i], buf[i+1] ) ) - (128<<8) );
                i++;
            }
        }
        return res;
    }
}
