// File: $Id$

class Life {
    /** Given the values the cell itself and its 8 neighbours, compute
     * the next state of the cell.
     * l=left, u=up, r=right, d=down, c=center
     */
    static byte computeNextState( byte lu, byte l, byte ld, byte u, byte c, byte d, byte ru, byte r, byte rd )
    {
        int neighbours = lu + l + ld + u + d + ru + r + rd;
        boolean alive = (neighbours == 3) || ((neighbours == 2) && (c==1));
        return alive?(byte) 1:(byte) 0;
    }
}
