/*
 * (c) copyright 1995 by the Vrije Universiteit, Amsterdam, The Netherlands.
 * For full copyright and restrictions on use see the file COPYRIGHT in the
 * top level of the Orca distribution.
 */


/*
 * Implementation in orca of Numerical Recipes ran1() but with integer result
 * in the range 0 .. 2^31 -1.
 * Assume 32 bit signed integers: 1/ otherwise the 3 simple random generators
 *				    overflow (or have a too small range);
 *				 2/ the combination of higher/lower bits is
 *				    wrong.
 */

#include <assert.h>

#include "gen_random.h"


#define table_size	97
#define two_16		65536
#define two_15		(two_16 / 2)
#define two_31		((double)two_16 * two_15)


static int	term[]	= { 54773,  28411,  51349 };
static int	fac[]	= { 7141,   8121,   4561 };
static int	mod[]	= { 259200, 134456, 243000 };

static double	inv_mod[3];
static int	current[3];
static double	table[table_size];


double
gen_random_val01(void)
{
    int		i;
    int		ix;
    double	r;

    for (i = 0; i < 3; i++) {
	current[i] = (fac[i] * current[i] + term[i]) % mod[i];
    }
    ix = (table_size * current[2]) / mod[2];
    r  = table[ix];
    table[ix] = (current[0] + current[1] * inv_mod[1]) * inv_mod[0];

    return r;
}


int
gen_random_val(void)
{
    return (int)(gen_random_val01() * two_31);
}


void
gen_random_set_seed(int seed)
{
    int		i;

    assert(seed >= 0);
    for (i = 0; i < 3; i++) {
	inv_mod[i] = 1.0 / mod[i];
    }

    current[0] = (term[0] + seed) % mod[0];
    current[0] = (fac[0] * current[0] + term[0]) % mod[0];
    current[1] = current[0] % mod[1];
    current[0] = (fac[0] * current[0] + term[0]) % mod[0];
    current[2] = current[0] % mod[2];

    for (i = 0; i < table_size; i++) {
	current[0] = (fac[0] * current[0] + term[0]) % mod[0];
	current[1] = (fac[1] * current[1] + term[1]) % mod[1];
	table[i]   = (current[0] + current[1] * inv_mod[1]) * inv_mod[0];
    }
}


void
gen_random_init(int *argc, char *argv[])
{
    gen_random_set_seed(1);
}


void
gen_random_end(void)
{
}
