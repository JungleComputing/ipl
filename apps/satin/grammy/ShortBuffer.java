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

    /** Returns a short array containing the current text in the buffer. */
    public short [] getText()
    {
        short res[] = new short[sz];

        for( int i=0; i<sz; i++ ){
            res[i] = buf[i];
        }
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

    public void append( int b )
    {
        reserve( sz+1 );
        buf[sz++] = (short) b;
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

    public short[] getShorts()
    {
        short res[] = new short[sz];

        System.arraycopy( buf, 0, res, 0, sz );
        return res;
    }

    public boolean isEqual( short a[] )
    {
        if( a.length != sz ){
            return false;
        }
        for( int i=0; i<sz; i++ ){
            if( a[i] != buf[i] ){
                return false;
            }
        }
        return true;
    }

    /** Expands the rule with the given code. */
    private void expandRule( short code, short rhs[] )
    {
        int occ = 0;
        final int rhslen = rhs.length;

        // First, count the number of occurences of this non-terminal.
        for( int i=0; i<sz; i++ ){
            if( buf[i] == code ){
                occ++;
            }
        }
        if( occ == 0 ){
            // This rule doesn't occur, don't bother.
            // TODO: we should complain, since in a well-constructed 
            // compression this shouldn't happen.
            return;
        }
        int newsz = sz+(rhslen-1)*occ;   // The expanded size.
        short newbuf[] = new short[newsz];

        int j = 0;
        for( int i=0; i<sz; i++ ){
            if( buf[i] == code ){
                System.arraycopy( rhs, 0, newbuf, j, rhslen );
                j += rhslen;
            }
            else {
                newbuf[j++] = buf[i];
            }
        }
        buf = newbuf;
        sz = newsz;
    }

    /** Expands all grammar rules in the text. */
    public void decompress()
    {
        int nonterminals = 0;

        // First, count the number of non-terminals.
        // (We need that later to deduce the numbers of the grammar rules.)
        for( int i=0; i<sz; i++ ){
            if( buf[i] == STOP ){
                nonterminals++;
            }
        }

        while( nonterminals>0 ){
            // Now keep decoding grammar rules from back to front until
            // there are none left.
            short code = (short) (FIRSTCODE + --nonterminals);

            int i = sz;

            // Now search the string from back to front for a rule.
            while( i>0 ){
                i--;
                if( buf[i] == STOP ){
                    // We have found a grammar rule.
                    int len = sz-(i+1);
                    short ruleRhs[] = new short[len];

                    System.arraycopy( buf, i+1, ruleRhs, 0, len );

                    // Remove the grammar rule from the buffer.
                    sz = i;
                    expandRule( code, ruleRhs );
                    break;
                }
            }
        }
    }
}
