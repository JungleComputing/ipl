class Position {
    int ply_of_game; /* must be even for black, odd for white */

    int ply; /* actual ply of search                  */

    int[] board;

    int inc_eval; /* current static evaluation             */

    int depth;

    int alpha;

    int beta;

    Position() {
        board = new int[0x80];
    }

    Position(Position orig) {
        ply_of_game = orig.ply_of_game;
        ply = orig.ply;
        board = (int[]) orig.board.clone(); /* really needed! */
        inc_eval = orig.inc_eval;
        depth = orig.depth;
        alpha = orig.alpha;
        beta = orig.beta;
    }

    void showbd() {
        int i;

        for (i = 0; i < 0x78; i++) {
            if ((i & 0x88) != 0) {
                System.out.println();
                i += 8;
            }
            switch (board[i]) {
            case 0:
                System.out.print(" -- ");
                break;
            case Checkers.WP:
                System.out.print(" WP ");
                break;
            case Checkers.WK:
                System.out.print(" WK ");
                break;
            case Checkers.BP:
                System.out.print(" BP ");
                break;
            case Checkers.BK:
                System.out.print(" BK ");
                break;
            }

        }

        System.out.println();
    }
}