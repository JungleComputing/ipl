// File: $Id$

class Life {
    static final int BOARDSIZE = 200;
    static final int GENERATIONS = 201;

    // The cells. There is a border of cells that are always empty,
    // but make the border conditions easy to handle.
    static byte oldboard[][] = new byte[BOARDSIZE+2][BOARDSIZE+2];
    static byte newboard[][] = new byte[BOARDSIZE+2][BOARDSIZE+2];

    // Compute a new generation from oldboard to newboard, and then
    // swap the two boards.
    private void nextGeneration()
    {
        for( int i=1; i<=BOARDSIZE; i++ ){
            for( int j=1; j<=BOARDSIZE; j++ ){
                int neighbours =
                    oldboard[i-1][j-1] +
                    oldboard[i-1][j] +
                    oldboard[i-1][j+1] +
                    oldboard[i][j-1] +
                    oldboard[i][j+1] +
                    oldboard[i+1][j-1] +
                    oldboard[i+1][j] +
                    oldboard[i+1][j+1];
                boolean alive = (neighbours == 3) || ((neighbours == 2) && (oldboard[i][j]==1));
                newboard[i][j] = alive?(byte) 1:(byte) 0;
            }
        }
        byte tmp[][] = oldboard;
        oldboard = newboard;
        newboard = tmp;
    }

    private byte horTwister[][] = {
        { 0, 0, 0, 0, 0 },
        { 0, 1, 1, 1, 0 },
        { 0, 0, 0, 0, 0 },
    };

    private byte vertTwister[][] = {
        { 0, 0, 0 },
        { 0, 1, 0 },
        { 0, 1, 0 },
        { 0, 1, 0 },
        { 0, 0, 0 },
    };

    private byte horTril[][] = {
        { 0, 0, 0, 0, 0, 0 },
        { 0, 0, 1, 1, 0, 0 },
        { 0, 1, 0, 0, 1, 0 },
        { 0, 0, 1, 1, 0, 0 },
        { 0, 0, 0, 0, 0, 0 },
    };

    private byte vertTril[][] = {
        { 0, 0, 0, 0, 0 },
        { 0, 0, 1, 0, 0 },
        { 0, 1, 0, 1, 0 },
        { 0, 1, 0, 1, 0 },
        { 0, 0, 1, 0, 0 },
        { 0, 0, 0, 0, 0 },
    };

    /**
     * Puts the given pattern at the given coordinates.
     * Since we want the pattern to be readable, we take the first
     * row of the pattern to be the at the top.
     */
    protected void putPattern( int px, int py, byte pat[][] )
    {
        for( int y=pat.length-1; y>=0; y-- ){
            byte paty[] = pat[y];

            for( int x=0; x<paty.length; x++ ){
                oldboard[px+x][py+y] = paty[x];
            }
        }
    }

    /**
     * Returns true iff the given pattern occurs at the given
     * coordinates.
     */
    protected boolean hasPattern( int px, int py, byte pat[][ ] )
    {
        for( int y=pat.length-1; y>=0; y-- ){
            byte paty[] = pat[y];

            for( int x=0; x<paty.length; x++ ){
                if( oldboard[px+x][py+y] != paty[x] ){
                    return false;
                }
            }
        }
        return true;
    }

    // Put a twister (a bar of 3 cells) at the given center cell.
    protected void putTwister( int x, int y )
    {
        putPattern( x-2, y-1, horTwister );
    }

    // Given a position, return true iff there is a twister in hor or
    // vertical position at that point.
    protected boolean hasTwister( int x, int y )
    {
        if( false ){
            if( oldboard[x][y] != 1 ){
                return false;
            }
            return true;
        }
        return hasPattern( x-2, y-1, horTwister ) ||
            hasPattern( x-1, y-2, vertTwister );
    }

    public static void main( String args[] )
    {
        Life game = new Life();
        game.putTwister( 100, 3 );

        System.out.println( "Started" );
        for( int n=0; n<GENERATIONS; n++ ){
            game.nextGeneration();
        }
        if( !game.hasTwister( 100, 3 ) ){
            System.out.println( "Twister has gone missing" );
        }
        System.out.println( "Done" );
    }
}
