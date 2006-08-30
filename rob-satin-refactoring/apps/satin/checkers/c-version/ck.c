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

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <memory.h>
#include "gen_random.h"

#define PAWN    1
#define KING    2
#define WHITE   4
#define BLACK   8

#define WP  WHITE+PAWN
#define BP  BLACK+PAWN
#define WK  WHITE+KING
#define BK  BLACK+KING

#define INF       900000
#define WIN_SCORE 100000

#define MAX_PLY 128

int root_move;
int verbose = 0;

int eval_tab[12] =
{
     0, 0, 0, 0,
     0, 10, 12, 0,
     0, 10, 12, 0
};

int color_eval_tab[12] =
{
     0, 0, 0, 0,
     0, -10, -12, 0,
     0, 10, 12, 0,
};

int dirtab[2][4] =
{
     {15, 17, -17, -15},
     {-15, -17, 15, 17}
};

int opening[0x80] =
{
     WP, 00, WP, 00, WP, 00, WP, 00, 0, 0, 0, 0, 0, 0, 0, 0,
     00, WP, 00, WP, 00, WP, 00, WP, 0, 0, 0, 0, 0, 0, 0, 0,
     WP, 00, WP, 00, WP, 00, WP, 00, 0, 0, 0, 0, 0, 0, 0, 0,
     00, 00, 00, 00, 00, 00, 00, 00, 0, 0, 0, 0, 0, 0, 0, 0,
     00, 00, 00, 00, 00, 00, 00, 00, 0, 0, 0, 0, 0, 0, 0, 0,
     00, BP, 00, BP, 00, BP, 00, BP, 0, 0, 0, 0, 0, 0, 0, 0,
     BP, 00, BP, 00, BP, 00, BP, 00, 0, 0, 0, 0, 0, 0, 0, 0,
     00, BP, 00, BP, 00, BP, 00, BP, 0, 0, 0, 0, 0, 0, 0, 0,
};

typedef struct {
     int ply_of_game;		/* must be even for black, odd for white */
     int ply;			/* actual ply of search                  */
     int board[0x80];
     int inc_eval;		/* current static evaluation             */
     int depth;
     int alpha;
     int beta;
} position;

int killer[MAX_PLY];

/* ---- PROTOTYPES ---- */

int srch(position * oldp, int mv);
void make(position * p, int mv);
void showbd(position * p);
void play(int blev, int wlev);

int usage()
{
     printf("\n");
     printf("This program plays rudimentary checkers, without double jumps. Both\n");
     printf("colors are played by the computer.\n\n"); 
     printf("Command: ck [<options>] \n\n");
     printf("Options: -d #  specifies how deep the game tree is evaluated\n");
     printf("               for black and white.");
     printf("         -w #  search depth for white in plys.\n");
     printf("         -b #  search depth for black in plys.\n");
     printf("         -h    for help\n");
     printf("         -benchmark [ short | medium | long ]\n\n");
     printf("Default: ck -b 7 -w 8\n");
     printf("Author:  Don Dailey, drd@supertech.lcs.mit.edu\n\n");
     return 1;
}

int main(int argc, char *argv[])
{
     int level;
     int blev;
     int wlev;
     int benchmark = 0;
     int help = 0;
     int i;
  
     gen_random_init(&argc, argv);

     /* standard benchmark options */
     wlev = 8;
     blev = 7;
     level = 0;
  
     for (i = 1; i < argc; i++) {
	     if (0) {
	     } else if (!strcmp(argv[i], "-d")) {
		     level = atoi(argv[i+1]);
		     i++;
	     } else if (!strcmp(argv[i], "-w")) {
		     wlev = atoi(argv[i+1]);
		     i++;
	     } else if (!strcmp(argv[i], "-b")) {
		     blev = atoi(argv[i+1]);
		     i++;
	     } else if (!strcmp(argv[i], "-v")) {
		     verbose = 1;
	     } else if (!strcmp(argv[i], "-h")) {
		     usage();
		     exit(1);
	     } else if (!strcmp(argv[i], "-benchmark")) {
		     benchmark = atoi(argv[i+1]);
		     i++;
	     } else {
		     printf("No such option: %s", argv[i]);
		     usage();
		     exit(1);
	     }
     }
     
     if (level!=0) wlev = blev = level;
     
     if (help) return usage();
  
     if (benchmark) {
          switch (benchmark) {
	  case 1:      /* short benchmark options -- a little work*/
	       wlev = 3;
	       blev = 3;
	       break;
	  case 2:      /* standard benchmark options*/
	       wlev = 8;
	       blev = 7;
	       break;
	  case 3:      /* long benchmark options -- a lot of work*/
	       wlev = 10;
	       blev = 10;
	       break;
	  }
     }

     if (wlev < 1) wlev = 1;
     if (wlev > 99) wlev = 99;
     if (blev < 1) blev = 1;
     if (blev > 99) blev = 99;

     printf("Options: black search plys = %d\n", blev);
     printf("         wite search plys  = %d\n", wlev);

     /* Timing. "Start" timers */
     play(blev, wlev);

     /* Timing. "Stop" timers */
     printf("done\n");

     gen_random_end();
  
     return (0);
}


void play(int blev, int wlev)
{
     position gme[200];		/* record of game */
     int ply;
     int i;
     int sd;
     int score = 0;		/* to get rid of warning error */
     int gameover = 0;
     int level;
     /* initialize the position state */

     /* 
      * NOTE: everything may look a little backwards because the
      * search immediately starts negamaxing                  
      */
     memcpy(gme[0].board, opening, sizeof(gme[0]));

     /* calculate static score */

     gme[0].inc_eval = 0;
     for (i = 0; i < 0x78; i++) {
	  if (i & 0x88)
	       i += 8;		/* skip through board edge */
	  gme[0].inc_eval += color_eval_tab[gme[0].board[i]];
     }

     gme[0].inc_eval = -gme[0].inc_eval;

     if(verbose) {
	     printf("Starting inc_eval = %8d\n", gme[0].inc_eval);
     }

     /* cycle through a game */

     if(verbose) {
	     showbd(&gme[0]);
     }

     for (ply = 0; ply < 120; ply++) {
	  /* initialize search */

	  gme[ply].ply_of_game = ply - 1;
	  gme[ply].ply = 0;

	  if (ply & 1)
	       level = wlev;
	  else
	       level = blev;

	  score = 0;

	  for (sd = 1; sd <= level; sd++) {
	       if (abs(score) > 1000 && sd > 1) {
		       if(verbose) {
			       printf("\n");
		       }
		    continue;
	       }
	       gme[ply].depth = sd;
	       gme[ply].alpha = -INF;
	       gme[ply].beta = INF;

	       score = srch(&gme[ply], 0);

	       if(verbose) {
		       printf("%4d)   sc: %-8d  mv=%5x\n",
			      sd,
			      score,
			      root_move
			       );
		       fflush(stdout);
	       }
	       if (score > 1000 && sd == 1)
		    gameover = 1;

	  }
	  if(verbose) {
		  printf("\n");
		  fflush(stdout);
	  }

	  gme[ply + 1] = gme[ply];
	  make(&gme[ply + 1], root_move);
	  if(verbose) {
		  showbd(&gme[ply + 1]);
	  }

	  if (gameover)
	       break;

	  /* next position */
	  gme[ply + 1].inc_eval = -gme[ply + 1].inc_eval;

     }
     showbd(&gme[ply]);

}

/* move generator */
int gen(position * p, int *mvl)
{
     int i;
     int ctm;			/* color to move  4 = black,   8 = white */
     int cix;			/* color index  0=black  1=white         */
     int count = 0;
     int dir;
     int dest;
     int cap = 0;		/* flag for captures */

     if (p->ply_of_game & 1) {
	  ctm = WHITE;
	  cix = 0;
     } else {
	  ctm = BLACK;
	  cix = 1;
     }

     for (i = 0; i < 0x78; i++) {
	  if (i & 0x88)
	       i += 8;		/* skip through board edge */

	  if (p->board[i] & ctm)
	       for (dir = 0; dir < 4; dir++) {
		    if (dir == 2 && (p->board[i] & PAWN))
			 break;
		    dest = i + dirtab[cix][dir];
		    if (dest & 0x88)
			 continue;
		    if (p->board[dest] == 0) {
			 mvl[count++] = (i << 8) + dest;
		    } else if (!(p->board[dest] & ctm)) {	/* jump
								 * move */
			 if ((dest + dirtab[cix][dir]) & 0x88)
			      continue;

			 if (p->board[dest + dirtab[cix][dir]] == 0) {
			      mvl[count++] = (i << 8) + dest;
			      cap++;
			 }
		    }
	       }
     }

     if (cap) {			/* cull out non-jump moves */
	  cap = 0;

	  for (i = 0; i < count; i++)
	       if (p->board[mvl[i] & 0xff])
		    mvl[cap++] = mvl[i];

	  count = cap;
     }
     /* randomize list at ply 1 */
     /* ----------------------- */

     if (p->ply == 1) {
	  /* 
	   * shuffle routine - forgive me for my blatant re-use of 
	   * variables here!  drd 
	   */

	  for (i = 0; i < count; i++) {
	       do
		    cix = (gen_random_val() & 31);
	       while (cix >= count);

	       cap = mvl[i];
	       mvl[i] = mvl[cix];
	       mvl[cix] = cap;
	  }

     }
     /* put killer move at front of list */

     cix = killer[p->ply];
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
 */
int srch(position * oldp, int mv)
{
     position p;
     int best_score = -INF;
     int move_list[32];
     int count;
     int x;
     int beta_cutoff = 0;
     int cur_score = 0;

     void catch(int eval, int choice_ix) {
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

     /* fix up new state */
     p = *oldp;

     /* nega-max everything */
     p.inc_eval = -p.inc_eval;
     x = p.alpha;
     p.alpha = -p.beta;
     p.beta = -x;

     p.ply_of_game++;
     p.depth--;
     p.ply++;

     make(&p, mv);		/* make the move */

     /* generate all legal moves from this position */
     count = gen(&p, move_list);

     if (count == 0)
	  return (-WIN_SCORE + p.ply);	/* game over */

     /* exhausted quies search? */

     if (p.depth < 0 && (p.board[move_list[0] & 0xff] == 0))
	  return (p.inc_eval);

     /* this loops tries all possible moves */
     for (x = 0; x < count; x++) {

	  /* search 1st move and root moves serially */

	  if (x == 0 || p.ply == 1) {
	       cur_score = srch(&p, move_list[x]);

	       cur_score = -cur_score;

	       if (cur_score > best_score) {
		    killer[p.ply] = move_list[x];
		    best_score = cur_score;
		    if (p.ply == 1)
			 root_move = move_list[x];
	       }
	       if (cur_score >= p.beta) {
		    killer[p.ply] = move_list[x];
		    return (best_score);
	       }
	       if (cur_score > p.alpha)
		    p.alpha = cur_score;

	       continue;
	  }
	  /* search all other moves in parallel */
	  catch(srch(&p, move_list[x]), x);
	  if (beta_cutoff)
	       break;
     }

     return (best_score);
}

void showbd(position * p)
{
     int i;

     for (i = 0; i < 0x78; i++) {
	  if (i & 0x88) {
	       printf("\n");
	       fflush(stdout);
	       i += 8;
	  }
	  switch (p->board[i]) {
	      case 0:
		   printf(" -- ");
		   break;
	      case WP:
		   printf(" WP ");
		   break;
	      case WK:
		   printf(" WK ");
		   break;
	      case BP:
		   printf(" BP ");
		   break;
	      case BK:
		   printf(" BK ");
		   break;
	  }

     }

     printf("\n\n");
     fflush(stdout);

}

void make(position * p, int mv)
{
     int mv_from;
     int mv_to;

     if (mv) {			/* else we are root  level */
	  mv_from = mv >> 8;
	  mv_to = mv & 0xff;

	  if (p->board[mv_to]) {	/* then its a capture */
	       p->inc_eval -= eval_tab[p->board[mv_to]];
	       p->board[mv_to] = 0;
	       mv_to = mv_to + (mv_to - mv_from);
	       p->board[mv_to] = p->board[mv_from];
	       p->board[mv_from] = 0;
	  } else {
	       p->board[mv_to] = p->board[mv_from];
	       p->board[mv_from] = 0;
	  }

	  /* detect promotion to KING  */
	  /* ------------------------  */

	  if (p->board[mv_to] == BP && mv_to < 8) {
	       p->board[mv_to] = BK;
	       p->inc_eval -= eval_tab[BK];
	       p->inc_eval += eval_tab[BP];
	  } else if (p->board[mv_to] == WP && mv_to >= 0x70) {
	       p->board[mv_to] = WK;
	       p->inc_eval -= eval_tab[WK];
	       p->inc_eval += eval_tab[WP];
	  }
     }
     return;
}
