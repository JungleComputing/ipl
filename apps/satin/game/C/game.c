/*
 * This is a simple game to test aborts.
 * Rules: There are two players on a n x m board. Alternatively,
 * each player puts a coin in position (i,j) and in all positions
 * (i',j') for all i' <= i and j' <= j (multiple coins count as one).
 * The player who fills the board loses.
 *
 * The first player has a winning strategy.
 *
 * I do no attempt to be clever in this program.
 *
 * This game was suggested by David Finberg, dfinberg@mit.edu .
 */

#include <stdio.h>
#include <stdlib.h>

#define M 6
#define N 6

#define EMPTY 0
#define COIN 1

#define WIN 1000
#define LOSE (-WIN)

typedef int BOARD[N][M];
typedef int (*BOARDHACK)[M];
typedef int INDEX;


int usage() 
/* Not very useful since there are no options, but anyway to be
 * consistent with the other examples.  
*/
{
     printf("\n"); 
     printf("This is a simple game to test aborts.\n");
     printf("Rules: There are two players on a n x m board. Alternatively,\n");
     printf("each player puts a coin in position (i,j) and in all positions\n");
     printf("(i',j') for all i' <= i and j' <= j (multiple coins count as one).\n");
     printf("The player who fills the board loses.\n\n");
     printf("The first player has a winning strategy.\n\n");
     printf("Command: game [<cilk-options>] [<options>] \n\n");
     printf("Options: -h    for this help\n");
      printf("Author:  Matteo Frigo, athena@theory.lcs.mit.edu\n\n");
     return 1;
}

inline void init_board(BOARD b)
{
     INDEX i, j;

     for (i = N - 1; i >= 0; --i) {
	     for (j = M - 1; j >= 0; --j) {
		     b[i][j] = EMPTY;
	     }
     }
}

/* test if the move is legal */
inline int legal(BOARD b, INDEX i, INDEX j)
{
     return (b[i][j] == EMPTY);
}

/* produce a new board nb with the given move */
inline void move(BOARD b, INDEX r, INDEX c, BOARD nb)
{
     INDEX i, j;

     for (i = N - 1; i >= 0; --i) {
	     for (j = M - 1; j >= 0; --j) {
		     nb[i][j] = b[i][j];
	     }
     }

     for (i = 0; i <= r; ++i)
	  for (j = 0; j <= c; ++j)
	       nb[i][j] = COIN;
}

/* return true if the position is lost */
inline int lostp(BOARD b)
{
     return (b[N - 1][M - 1] == COIN);
}

/* do the negamax search */
int negamax(BOARDHACK b, INDEX * movei, INDEX * movej,
		 int alpha, int beta, int depth)
{
     INDEX i, j;
     INDEX dummyi, dummyj;	/* not used */
     int done;
     int evaluation;

//     fprintf(stderr, ".");

     if (lostp(b))
	  return WIN - depth;	/* winning earlier is better */

     done = 0;

     for (i = N - 1; i >= 0; --i) {
	     for (j = M - 1; j >= 0; --j) {
		     if (legal(b, i, j)) {
			     int (*nb)[M] = alloca(sizeof(BOARD));
			     
			     move(b, i, j, nb);
			     evaluation = -negamax(nb, &dummyi, &dummyj, -beta, -alpha,
						  depth + 1);
			     
			     if (evaluation > alpha) {
				     alpha = evaluation;
				     /* record the best so far */
				     /* TODO: how do we communicate new alpha to children? */
				     *movei = i;
				     *movej = j;
			     }
			     if (alpha >= beta) {
				     done = 1; /* abort */
			     }
			     
			     if (done)
				     break;
		     }
	     }
     }
     return alpha;
}

int main(int argc, char *argv[])
{
     INDEX i, j;
     int (*b)[M];
     int best = 1;
     int first = 1;
     int prev_best = 1;
     int player;
     
     b = malloc(sizeof(BOARD));

     init_board(b);
     player = 0;

     while (!lostp(b)) {
          player = !(player);
	  best = negamax(b, &i, &j, -100000, 100000, 0);

	  printf("%s found best move at %d,%d\n", ((player)?"White":"Black"), i, j);

	  first = 0;
	  prev_best = best;

	  move(b, i, j, b);
	  for (i = 0; i < N; ++i) {
	       for (j = 0; j < M; ++j)
		    printf("%d ", b[i][j]);
	       printf("\n");
	  }
	  printf("\n"); 
     }

     printf("%s wins\n\n", ((!(player))?"White":"Black"));

     return 0;
}
