/* $Id$ */

class Ecology {
    static private boolean haveAtLeastState( int v, byte lu, byte l, byte ld, byte u, byte d, byte ru, byte r, byte rd )
    {
        return (lu>=v) || (l>=v) || (ld>=v) || (u>=v) || (d>=v) || (ru>=v) || (r>=v) || (rd>=v);
    }

    // Compute the next state of a cell.
    // l=left, u=up, r=right, d=down, c=center
    static public byte computeNextState( int iteration, int row, int col, byte lu, byte l, byte ld, byte u, byte c, byte d, byte ru, byte r, byte rd )
    {
        // Value   State
        // 0       Wasteland
        // 1       Grassland
        // 2       Intermediate
        // 3..19   Forest 
        // 20      Burning forest
        // If any of the neighbours is on fire, and we are forest,
        // we get on fire.
        if( c >= 20 ){
            // Burning forest is now a wasteland.
            return 0;
        }
        if( c == 0 && haveAtLeastState( 1, lu, l, ld, u, d, ru, r, rd ) ){
             // Grass from any of the neighbours invades wasteland.
             return 1;
        }
        if( c == 1 && haveAtLeastState( 2, lu, l, ld, u, d, ru, r, rd ) ){
             // Clumps of trees from any of the neighbours invades Grassland.
             return 2;
        }
        if( c == 2 && haveAtLeastState( 3, lu, l, ld, u, d, ru, r, rd ) ){
             // Forest from any of the neighbours invades Intermediate.
             return 3;
        }
        if( c >= 3 && haveAtLeastState( 20, lu, l, ld, u, d, ru, r, rd ) ){
            // Forest fire in one of our neighbours spreads to this woodland.
            return 20;
        }
        // Forest ages.
        return (byte) (c+1);
    }

    static void putForest( byte board[][], int dx, int dy )
    {
        for( int x=1; x<board.length; x += dx ){
            byte col[] = board[x];

            if( col != null ){
                for( int y=1; y<col.length; y += dy ){
                    col[y] = 3;
                }
            }
        }
    }
}

