// File: $Id$

class Backref implements java.io.Serializable {
    int backpos;
    int pos;
    int len;
    int extraGain;

    public Backref( int backpos, int pos, int len )
    {
        this.backpos = backpos;
        this.pos = pos;
        this.len = len;
    }

    /** Returns a new backref that represents a character copy. */
    public static Backref buildCopyBackref( int pos )
    {
        return new Backref( -1, pos, -1 );
    }

    /** Returns true iff this backref represents a character copy. */
    public boolean isCopyBackref()
    {
        return len == -1;
    }

    /** Returns the encoding cost of this backref. */
    public int getCost()
    {
        return Helpers.refEncodingSize( pos-backpos, len );
    }

    /** Returns the gain of this backref. */
    public int getGain()
    {
        return extraGain+len-getCost();
    }

    /** Given an amount of gain, registers this as extra gain of this
     * backreference. This extra gain is presumably discovered by lookahead.
     */
    public void addGain( int g )
    {
        extraGain += g;
    }

    /** Given an amount of gain, registers this as extra gain of this
     * backreference. This extra gain is presumably discovered by lookahead.
     */
    public void addGain( Backref r )
    {
        extraGain += r.getGain();
    }

    public String toString()
    {
        if( len == -1 ){
            return "@" + pos + " (copy)";
        }
        return "@" + pos + "->" + backpos + " len=" + len +  " gain=" + getGain();
    }
}
