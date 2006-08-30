/* $Id$ */

interface Datatype {

    static final int BYTE	= 1;
    static final int INT	= BYTE + 1;
    static final int FLOAT	= INT + 1;
    static final int DOUBLE	= FLOAT + 1;
    static final int TREE	= DOUBLE + 1;
    static final int CYCLIC	= TREE + 1;
    static final int TWO_INT	= CYCLIC + 1;
    static final int INT_32	= TWO_INT + 1;
    static final int B		= INT_32 + 1;
    static final int INNER	= B + 1;
    static final int SWITCHER	= INNER + 1;

}
