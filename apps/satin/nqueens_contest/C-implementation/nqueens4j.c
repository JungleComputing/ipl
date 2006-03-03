/**************************************************************************/
/* N-Queens Solutions  ver3.1               takaken July/2003             */
/**************************************************************************/
#include <stdio.h>
#include <time.h>

#define  MAXSIZE  17 
#define  MINSIZE  17 

int  SIZE, SIZEE;
int  BOARD[MAXSIZE], *BOARDE, *BOARD1, *BOARD2;
int  TOPBIT, ENDBIT;

long  TOTAL, UNIQUE;

/**********************************************/
/* Display the Board Image                    */
/**********************************************/
void Display(void)
{
    int  y, bit;

    printf("N= %d\n", SIZE);
    for (y=0; y<SIZE; y++) {
        for (bit=TOPBIT; bit; bit>>=1)
            printf("%s ", (BOARD[y] & bit)? "Q": "-");
        printf("\n");
    }
    printf("\n");
}
/**********************************************/
/* Check Unique Solutions                     */
/**********************************************/
long Check(void)

{
    int  *own, *you, bit, ptn;

    /* 90-degree rotation */
    if (*BOARD2 == 1) {
        for (ptn=2,own=BOARD+1; own<=BOARDE; own++,ptn<<=1) {
            bit = 1;
            for (you=BOARDE; *you!=ptn && *own>=bit; you--)
                bit <<= 1;
            if (*own > bit) return 0;
            if (*own < bit) break;
        }
        if (own > BOARDE) {
            //Display();
            return 2;
        }
    }

    /* 180-degree rotation */
    if (*BOARDE == ENDBIT) {
        for (you=BOARDE-1,own=BOARD+1; own<=BOARDE; own++,you--) {
            bit = 1;
            for (ptn=TOPBIT; ptn!=*you && *own>=bit; ptn>>=1)
                bit <<= 1;
            if (*own > bit) return 0;
            if (*own < bit) break;
        }
        if (own > BOARDE) {
            //Display();
            return 4;
        }
    }

    /* 270-degree rotation */
    if (*BOARD1 == TOPBIT) {
        for (ptn=TOPBIT>>1,own=BOARD+1; own<=BOARDE; own++,ptn>>=1) {
            bit = 1;
            for (you=BOARD; *you!=ptn && *own>=bit; you++)
                bit <<= 1;
            if (*own > bit) return 0;
            if (*own < bit) break;
        }
    }
    //Display();
	return 8;
}
/**********************************************/
/* First queen is inside                      */
/**********************************************/
long Backtrack2(int y, int left, int down, int right, int mask, int lastmask, int sidemask, int bound1, int bound2)
{
    int  bitmap, bit;
	long lnsol = 0;
	int kids = 0;

    bitmap = mask & ~(left | down | right);
    if (y == SIZEE) {
        if (bitmap) {
            if (!(bitmap & lastmask)) {
                BOARD[y] = bitmap;
                lnsol += Check();
		 BOARD[y] = 0;
            }
        }
    } else {
        if (y < bound1) {
            bitmap |= sidemask;
            bitmap ^= sidemask;
        } else if (y == bound2) {
            if (!(down & sidemask)) return lnsol;
            if ((down & sidemask) != sidemask) bitmap &= sidemask;
        }
        while (bitmap) {
	    bit = -bitmap & bitmap;
	    BOARD[y] = bit;
            bitmap ^= bit; 
            lnsol += Backtrack2(y+1, (left | bit)<<1, down | bit, (right | bit)>>1, mask, lastmask, sidemask, bound1, bound2);
	    //kids ++;
        }
	//printf("I have %d kids\n", kids);
    }
	return lnsol;
}
/**********************************************/
/* First queen is in the corner               */
/**********************************************/
long Backtrack1(int y, int left, int down, int right, int bound1, int mask, int sizee)
{
    int  bitmap, bit;
    long  lnsol = 0;
    int kids = 0;

    bitmap = mask & ~(left | down | right);
    if (y == sizee) {
        if (bitmap) {
            //BOARD[y] = bitmap;
	    //COUNT8 += 8;
            return 8;	  
            //Display();
        }
	return 0;
    } else {
        if (y < bound1) {
            bitmap |= 2;
            bitmap ^= 2;
        }
	/*MyRemark: do we know how many children will be spawn? if not, we can't use fix-size array*/
       while (bitmap) {
            bitmap ^= bit = -bitmap & bitmap;
            lnsol += Backtrack1(y+1, (left | bit)<<1, down | bit, (right | bit)>>1, bound1, mask, sizee);
	    //kids ++;
        }
	//printf("I have %d kids\n", kids);
	return lnsol;
    }
}
/**********************************************/
/* Search of N-Queens                         */
/**********************************************/
void NQueens(void)
{
    int  bit;
    long  b1sol = 0;
    int MASK;
    int LASTMASK;
    int SIDEMASK;
    int BOUND1;
    int BOUND2;

    /* Initialize */
    SIZEE  = SIZE - 1;
    BOARDE = &BOARD[SIZEE];
    TOPBIT = 1 << SIZEE;
    MASK   = (1 << SIZE) - 1;

    /* 0:000000001 */
    /* 1:011111100 */

    /* 0:000001110 */
    SIDEMASK = LASTMASK = TOPBIT | 1;
    ENDBIT = TOPBIT >> 1;


    for (BOUND1=1,BOUND2=SIZE-2; BOUND1<BOUND2; BOUND1++,BOUND2--) {
        BOARD1 = &BOARD[BOUND1];
        BOARD2 = &BOARD[BOUND2];
        BOARD[0] = bit = 1 << BOUND1;
        b1sol += Backtrack2(1, bit<<1, bit, bit>>1, MASK, LASTMASK, SIDEMASK, BOUND1, BOUND2);
	//printf("%d\n", b1sol);
        LASTMASK |= LASTMASK>>1 | LASTMASK<<1;
        ENDBIT >>= 1;
    }

/*
	BOUND1 = 2;
	BOUND2 = SIZE - 3;
 	LASTMASK |= LASTMASK>>1 | LASTMASK<<1;
        ENDBIT >>= 1;
	BOARD1 = &BOARD[BOUND1];
        BOARD2 = &BOARD[BOUND2];
        BOARD[0] = bit = 1 << BOUND1;
        b1sol += Backtrack2(1, bit<<1, bit, bit>>1, MASK, LASTMASK, SIDEMASK, BOUND1, BOUND2);
        printf("%d\n", b1sol);
                                              
 */                   

    BOARD[0] = 1;
    for (BOUND1=2; BOUND1<SIZEE; BOUND1++) {
        BOARD[1] = bit = 1 << BOUND1;
        b1sol += Backtrack1(2, (2 | bit)<<1, 1 | bit, bit>>1, BOUND1, MASK, SIZEE);
    }

    /* Unique and Total Solutions */
    UNIQUE =  b1sol;
    TOTAL  = UNIQUE;

}
/**********************************************/
/* Format of Used Time                        */
/**********************************************/
void TimeFormat(clock_t utime)
{
    int  dd, hh, mm;
    float ftime, ss;

    ftime = (float)utime / CLOCKS_PER_SEC;

    mm = (int)ftime / 60;
    ss = ftime - (float)(mm * 60);
    dd = mm / (24 * 60);
    mm = mm % (24 * 60);
    hh = mm / 60;
    mm = mm % 60;

    if (dd) printf("%4d %02d:%02d:%05.2f\n", dd, hh, mm, ss);
    else if (hh) printf("     %2d:%02d:%05.2f\n", hh, mm, ss);
    else if (mm) printf("        %2d:%05.2f\n", mm, ss);
    else printf("           %5.2f\n", ss);
}
/**********************************************/
/* N-Queens Solutions MAIN                    */
/**********************************************/
int main(void)
{
    clock_t starttime;
    clock_t endtime;	

    printf("<------  N-Queens Solutions  -----> <---- time ---->\n");
    printf(" N:           Total          Unique days hh:mm:ss.--\n");
    for (SIZE=MINSIZE; SIZE<=MAXSIZE; SIZE++) {
        starttime = clock();
        NQueens();
	endtime = clock();
	printf("%2d:%8d\t%8d", SIZE, TOTAL, UNIQUE); fflush(stdout);
	TimeFormat(endtime - starttime);	
    }

    return 0;
}
