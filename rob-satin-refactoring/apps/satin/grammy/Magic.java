// File: $Id$

/** Magical byte values for compression. */
interface Magic {
    static final short ESCAPE1 = 255;
    static final short ESCAPE2 = 254;
    static final short STOP = -1;
    static final short FIRSTCODE = 256;
}
