/*
 * (c) copyright 1995 by the Vrije Universiteit, Amsterdam, The Netherlands.
 * For full copyright and restrictions on use see the file COPYRIGHT in the
 * top level of the Orca distribution.
 */

#ifndef __GEN_RANDOM_H__
#define __GEN_RANDOM_H__

/*
 * Copied from
 * Implementation in orca of Numerical Recipes ran1() but with integer result
 * in the range 0 .. 2^31 -1.
 * Assume 32 bit signed integers: 1/ otherwise the 3 simple random generators
 *				    overflow (or have a too small range);
 *				 2/ the combination of higher/lower bits is
 *				    wrong.
 */


int gen_random_val(void);
double gen_random_val01(void);
void gen_random_set_seed(int seed);

void gen_random_init(int *argc, char *argv[]);
void gen_random_end(void);

#endif
