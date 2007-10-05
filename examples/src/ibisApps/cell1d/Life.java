package ibisApps.cell1d;

// File: $Id$

class Life {
    /** Given the values the cell itself and its 8 neighbours, compute
     * the next state of the cell.
     * l=left, u=up, r=right, d=down, c=center
     */
    static byte computeNextState( int iteration, int row, int col, byte lu, byte l, byte ld, byte u, byte c, byte d, byte ru, byte r, byte rd )
    {
        int neighbours = lu + l + ld + u + d + ru + r + rd;
        boolean alive = (neighbours == 3) || ((neighbours == 2) && (c==1));
        return alive?(byte) 1:(byte) 0;
    }

    private static byte horTwister[][] = {
        { 0, 0, 0, 0, 0 },
        { 0, 1, 1, 1, 0 },
        { 0, 0, 0, 0, 0 },
    };

    private static byte vertTwister[][] = {
        { 0, 0, 0 },
        { 0, 1, 0 },
        { 0, 1, 0 },
        { 0, 1, 0 },
        { 0, 0, 0 },
    };

    @SuppressWarnings("unused")
    private static byte horTril[][] = {
        { 0, 0, 0, 0, 0, 0 },
        { 0, 0, 1, 1, 0, 0 },
        { 0, 1, 0, 0, 1, 0 },
        { 0, 0, 1, 1, 0, 0 },
        { 0, 0, 0, 0, 0, 0 },
    };

    @SuppressWarnings("unused")
    private static byte vertTril[][] = {
        { 0, 0, 0, 0, 0 },
        { 0, 0, 1, 0, 0 },
        { 0, 1, 0, 1, 0 },
        { 0, 1, 0, 1, 0 },
        { 0, 0, 1, 0, 0 },
        { 0, 0, 0, 0, 0 },
    };

    private static byte glider[][] = {
        { 0, 0, 0, 0, 0 },
        { 0, 1, 1, 1, 0 },
        { 0, 1, 0, 0, 0 },
        { 0, 0, 1, 0, 0 },
        { 0, 0, 0, 0, 0 },
    };

    /**
     * Puts the given pattern at the given coordinates.
     * Since we want the pattern to be readable, we take the first
     * row of the pattern to be the at the top.
     */
    static protected void putPattern( byte board[][], int px, int py, byte pat[][] )
    {
        for( int y=pat.length-1; y>=0; y-- ){
            byte paty[] = pat[y];

            for( int x=0; x<paty.length; x++ ){
                if( board[px+x] != null ){
                    board[px+x][py+y] = paty[x];
                }
            }
        }
    }

    /**
     * Returns true iff the given pattern occurs at the given
     * coordinates.
     */
    static protected boolean hasPattern( byte board[][], int px, int py, byte pat[][ ] )
    {
        for( int y=pat.length-1; y>=0; y-- ){
            byte paty[] = pat[y];

            for( int x=0; x<paty.length; x++ ){
                if( board[px+x] != null && board[px+x][py+y] != paty[x] ){
                    return false;
                }
            }
        }
        return true;
    }

    // Put a twister (a bar of 3 cells) at the given center cell.
    static void putTwister( byte board[][], int x, int y )
    {
        putPattern( board, x-2, y-1, horTwister );
    }

    // Put a glider at the given center cell.
    static void putGlider( byte board[][], int x, int y )
    {
        putPattern( board, x, y, glider );
    }

    // Given a position, return true iff there is a twister in hor or
    // vertical position at that point.
    static protected boolean hasTwister( byte board[][], int x, int y )
    {
        return hasPattern( board, x-2, y-1, horTwister ) ||
            hasPattern( board, x-1, y-2, vertTwister );
    }

}
