/*
 * This program plays rudimentary checkers, without double jumps. It is
 * not interactive, both colors are played by the computer. Also a good
 * test for aborts.
 *
 * Author: Don Dailey, drd@supertech.lcs.mit.edu
 *
 * Copyright (c) 1996 Massachusetts Institute of Technology
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the        Phone (617) 253-5094

 * "Software"), to use, copy, modify, and distribute the Software without
 * restriction, provided the Software, including any modified copies made
 * under this license, is not distributed for a fee, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE MASSACHUSETTS INSTITUTE OF TECHNOLOGY BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * Except as contained in this notice, the name of the Massachusetts
 * Institute of Technology shall not be used in advertising or otherwise
 * to promote the sale, use or other dealings in this Software without
 * prior written authorization from the Massachusetts Institute of
 * Technology.
 *  
 */

///@@@ try: make function of inlet, move try_catch out of srch
public final class Checkers extends ibis.satin.SatinObject implements
        CheckersInterface, java.io.Serializable {
    static final int PAWN = 1;

    static final int KING = 2;

    static final int WHITE = 4;

    static final int BLACK = 8;

    static final int WP = WHITE + PAWN;

    static final int BP = BLACK + PAWN;

    static final int WK = WHITE + KING;

    static final int BK = BLACK + KING;

    static final int INF = 900000;

    static final int WIN_SCORE = 100000;

    static final int MAX_PLY = 128;

    int root_move;

    int[] killer = new int[MAX_PLY];

    OrcaRandom rand = new OrcaRandom();

    static boolean verbose = false;

    int[] eval_tab = { 0, 0, 0, 0, 0, 10, 12, 0, 0, 10, 12, 0 };

    int[] color_eval_tab = { 0, 0, 0, 0, 0, -10, -12, 0, 0, 10, 12, 0, };

    int dirtab[][] = { { 15, 17, -17, -15 }, { -15, -17, 15, 17 } };

    int[] opening = { WP, 00, WP, 00, WP, 00, WP, 00, 0, 0, 0, 0, 0, 0, 0, 0,
            00, WP, 00, WP, 00, WP, 00, WP, 0, 0, 0, 0, 0, 0, 0, 0, WP, 00, WP,
            00, WP, 00, WP, 00, 0, 0, 0, 0, 0, 0, 0, 0, 00, 00, 00, 00, 00, 00,
            00, 00, 0, 0, 0, 0, 0, 0, 0, 0, 00, 00, 00, 00, 00, 00, 00, 00, 0,
            0, 0, 0, 0, 0, 0, 0, 00, BP, 00, BP, 00, BP, 00, BP, 0, 0, 0, 0, 0,
            0, 0, 0, BP, 00, BP, 00, BP, 00, BP, 00, 0, 0, 0, 0, 0, 0, 0, 0,
            00, BP, 00, BP, 00, BP, 00, BP, 0, 0, 0, 0, 0, 0, 0, 0, };

    static void usage() {
        System.out
                .println("This program plays rudimentary checkers, without double jumps. Both");
        System.out.println("colors are played by the computer.\n");
        System.out.println("Usage: java Checkers [options]");
        System.out
                .println("Options: -d #  specifies how deep the game tree is evaluated");
        System.out.println("               for black and white.");
        System.out.println("         -w #  search depth for white in plys.");
        System.out.println("         -b #  search depth for black in plys.");
        System.out.println("         -h    for help\n");
        System.out.println("         -benchmark [ 1 | 2 | 3 ]\n\n");
        System.out.println("Default: ck -b 7 -w 8\n");
    }

    public static void main(String[] args) {
        int level;
        int blev;
        int wlev;
        int benchmark = -1;
        long start, end;

        /* standard benchmark options */
        wlev = 8;
        blev = 7;
        level = 0;

        for (int i = 0; i < args.length; i++) {
            if (false) {
            } else if (args[i].equals("-d")) {
                level = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-w")) {
                wlev = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-b")) {
                blev = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-v")) {
                verbose = true;
            } else if (args[i].equals("-h")) {
                usage();
                System.exit(1);
            } else if (args[i].equals("-benchmark")) {
                benchmark = Integer.parseInt(args[i + 1]);
                i++;
            } else {
                System.err.println("No such option: " + args[i]);
                usage();
                System.exit(1);
            }
        }

        if (level != 0)
            wlev = blev = level;

        if (benchmark > 0) {
            switch (benchmark) {
            case 1: /* short benchmark options -- a little work*/
                wlev = 3;
                blev = 3;
                break;
            case 2: /* standard benchmark options*/
                wlev = 8;
                blev = 7;
                break;
            case 3: /* long benchmark options -- a lot of work*/
                wlev = 10;
                blev = 10;
                break;
            }
        }

        if (wlev < 1)
            wlev = 1;
        if (wlev > 99)
            wlev = 99;
        if (blev < 1)
            blev = 1;
        if (blev > 99)
            blev = 99;

        Checkers checkers = new Checkers();

        System.out.println("application checkers (" + blev + ", " + wlev
                + ") started");

        start = System.currentTimeMillis();
        checkers.play(blev, wlev);
        end = System.currentTimeMillis();
        double time = (double) (end - start) / 1000.0;

        System.out.println("application checkers (" + blev + ", " + wlev
                + ") took " + time + " seconds");
    }

    void play(int blev, int wlev) {
        Position[] gme = new Position[200]; /* record of game */
        int ply;
        int i;
        int sd;
        int score = 0;
        boolean gameover = false;
        int level = 0;

        /* initialize the position state */
        for (int k = 0; k < 200; k++) {
            gme[k] = new Position();
        }

        /* 
         * NOTE: everything may look a little backwards because the
         * search immediately starts negamaxing                  
         */
        gme[0].board = (int[]) opening.clone();

        /* calculate static score */

        gme[0].inc_eval = 0;
        for (i = 0; i < 0x78; i++) {
            if ((i & 0x88) != 0)
                i += 8; /* skip through board edge */
            gme[0].inc_eval += color_eval_tab[gme[0].board[i]];
        }

        gme[0].inc_eval = -gme[0].inc_eval;

        if (verbose) {
            System.out.println("Starting inc_eval = " + gme[0].inc_eval);
        }

        /* cycle through a game */

        if (verbose) {
            gme[0].showbd();
        }

        for (ply = 0; ply < 120; ply++) {
            /* initialize search */

            gme[ply].ply_of_game = ply - 1;
            gme[ply].ply = 0;

            if ((ply & 1) != 0)
                level = wlev;
            else
                level = blev;

            score = 0;

            for (sd = 1; sd <= level; sd++) {
                if (Math.abs(score) > 1000 && sd > 1) {
                    if (verbose) {
                        System.out.println();
                    }
                    continue;
                }
                gme[ply].depth = sd;
                gme[ply].alpha = -INF;
                gme[ply].beta = INF;

                try {
                    spawn_srch(gme[ply], 0, 0);
                } catch (Result r) {
                    score = r.score;
                    return;
                }
                sync();

                if (verbose) {
                    System.out.println(sd + ")    score = " + score + "  mv = "
                            + Integer.toHexString(root_move));
                }

                if (score > 1000 && sd == 1)
                    gameover = true;

            }
            if (verbose) {
                System.out.println();
            }

            gme[ply + 1] = gme[ply];
            make(gme[ply + 1], root_move);
            if (verbose) {
                gme[ply + 1].showbd();
            }

            if (gameover)
                break;

            /* next position */
            gme[ply + 1].inc_eval = -gme[ply + 1].inc_eval;
        }

        System.out.println("result:");
        gme[ply].showbd();
    }

    /* move generator */
    int gen(Position p, int[] mvl) {
        int i;
        int ctm; /* color to move  4 = black,   8 = white */
        int cix; /* color index  0=black  1=white         */
        int count = 0;
        int dir;
        int dest;
        int cap = 0; /* flag for captures */

        if ((p.ply_of_game & 1) != 0) {
            ctm = WHITE;
            cix = 0;
        } else {
            ctm = BLACK;
            cix = 1;
        }

        for (i = 0; i < 0x78; i++) {
            if ((i & 0x88) != 0)
                i += 8; /* skip through board edge */

            if ((p.board[i] & ctm) != 0)
                for (dir = 0; dir < 4; dir++) {
                    if (dir == 2 && ((p.board[i] & PAWN) != 0))
                        break;
                    dest = i + dirtab[cix][dir];
                    if ((dest & 0x88) != 0)
                        continue;
                    if (p.board[dest] == 0) {
                        mvl[count++] = (i << 8) + dest;
                    } else if ((p.board[dest] & ctm) == 0) { /* jump
                     * move */
                        if (((dest + dirtab[cix][dir]) & 0x88) != 0)
                            continue;

                        if (p.board[dest + dirtab[cix][dir]] == 0) {
                            mvl[count++] = (i << 8) + dest;
                            cap++;
                        }
                    }
                }
        }

        if (cap != 0) { /* cull out non-jump moves */
            cap = 0;

            for (i = 0; i < count; i++)
                if ((p.board[mvl[i] & 0xff]) != 0)
                    mvl[cap++] = mvl[i];

            count = cap;
        }
        /* randomize list at ply 1 */
        /* ----------------------- */

        if (p.ply == 1) {
            /* 
             * shuffle routine - forgive me for my blatant re-use of 
             * variables here!  drd 
             */

            for (i = 0; i < count; i++) {
                do
                    cix = (rand.nextInt() & 31);
                while (cix >= count);

                cap = mvl[i];
                mvl[i] = mvl[cix];
                mvl[cix] = cap;
            }

        }
        /* put killer move at front of list */

        cix = killer[p.ply];
        for (i = 0; i < count; i++)
            if (cix == mvl[i]) {
                cap = mvl[i];
                mvl[i] = mvl[0];
                mvl[0] = cix;
                break;
            }
        return (count);
    }

    /*
     * this is the negamax search code, with alpha-beta pruning.
     * Notice the usage of the inlet and of abort when beta cutoff occurs.
     */

    /* 
     changes: best_score, killer (global!), p, root_move (global), beta_cutoff
     reads: move_list
     */
    /*
     inlet void catch(int eval, int choice_ix) {
     eval = -eval;
     if (eval > best_score) {
     best_score = eval;
     killer[p.ply] = move_list[choice_ix];
     if (p.ply == 1)
     root_move = move_list[choice_ix];
     if (eval > p.alpha)
     p.alpha = eval;
     }
     if (eval >= p.beta) {
     beta_cutoff = 1;
     abort;
     }
     }
     */
    public void spawn_srch(Position oldp, int mv, int choice_ix) throws Result {
        srch(oldp, mv, choice_ix);
    }

    public void srch(Position oldp, int mv, int choice_ix) throws Result {
        Position p;
        int best_score = -INF;
        int[] move_list = new int[32];
        int count;
        int x;
        int beta_cutoff = 0;
        int cur_score = 0;

        /* fix up new state */
        p = new Position(oldp);

        Result r = new Result();

        /* nega-max everything */
        p.inc_eval = -p.inc_eval;
        x = p.alpha;
        p.alpha = -p.beta;
        p.beta = -x;

        p.ply_of_game++;
        p.depth--;
        p.ply++;

        make(p, mv); /* make the move */

        /* generate all legal moves from this position */
        count = gen(p, move_list);

        if (count == 0) {
            r.score = -WIN_SCORE + p.ply; /* game over */
            r.choice_ix = choice_ix;
            throw r;
        }

        /* exhausted quies search? */
        if (p.depth < 0 && (p.board[move_list[0] & 0xff] == 0)) {
            r.score = p.inc_eval;
            r.choice_ix = choice_ix;
            throw r;
        }

        /* this loops tries all possible moves */
        for (x = 0; x < count; x++) {
            /* search 1st move and root moves serially */
            if (x == 0 || p.ply == 1) {
                try {
                    srch(p, move_list[x], x);
                } catch (Result res) {
                    cur_score = res.score;
                    return;
                }

                cur_score = -cur_score;

                if (cur_score > best_score) {
                    killer[p.ply] = move_list[x];
                    best_score = cur_score;
                    if (p.ply == 1)
                        root_move = move_list[x];
                }
                if (cur_score >= p.beta) {
                    killer[p.ply] = move_list[x];
                    r.score = best_score;
                    r.choice_ix = choice_ix;
                    throw r;
                }
                if (cur_score > p.alpha)
                    p.alpha = cur_score;

                continue;
            }

            /* search all other moves in parallel */
            /* catch(spawn srch(&p, move_list[x]), x); */

            try {
                spawn_srch(p, move_list[x], x);
            } catch (Result res) { /* inlet code */
                int eval = -res.score;

                if (eval > best_score) {
                    best_score = eval;
                    killer[p.ply] = move_list[res.choice_ix];
                    if (p.ply == 1)
                        root_move = move_list[res.choice_ix];
                    if (eval > p.alpha)
                        p.alpha = eval;
                }
                if (eval >= p.beta) {
                    beta_cutoff = 1;
                    abort();
                }
                return;
            }

            if (beta_cutoff != 0)
                break;
        }

        sync();

        r.score = best_score;
        r.choice_ix = choice_ix;
        throw r;
    }

    void make(Position p, int mv) {
        int mv_from;
        int mv_to;

        if (mv != 0) { /* else we are root  level */
            mv_from = mv >> 8;
            mv_to = mv & 0xff;

            if (p.board[mv_to] != 0) { /* then its a capture */
                p.inc_eval -= eval_tab[p.board[mv_to]];
                p.board[mv_to] = 0;
                mv_to = mv_to + (mv_to - mv_from);
                p.board[mv_to] = p.board[mv_from];
                p.board[mv_from] = 0;
            } else {
                p.board[mv_to] = p.board[mv_from];
                p.board[mv_from] = 0;
            }

            /* detect promotion to KING  */
            /* ------------------------  */

            if (p.board[mv_to] == BP && mv_to < 8) {
                p.board[mv_to] = BK;
                p.inc_eval -= eval_tab[BK];
                p.inc_eval += eval_tab[BP];
            } else if (p.board[mv_to] == WP && mv_to >= 0x70) {
                p.board[mv_to] = WK;
                p.inc_eval -= eval_tab[WK];
                p.inc_eval += eval_tab[WP];
            }
        }
        return;
    }
}