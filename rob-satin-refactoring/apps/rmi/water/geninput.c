#include <stdio.h>
#include <stdlib.h>
#include <math.h>

typedef enum
{
    max_molecules = 4096 /* 343 */
}
molecule_type;

typedef enum
{
    position	 = 0,
    velocity	 = 1,
    acceleration = 2,
    force	 = 7,
    max_order	 = 8
}
order_type;

typedef enum
{
    xdir,
    ydir,
    zdir,
    nr_dirs = 3
}
direction_type;

typedef enum
{
    H1,
    O,
    H2,
    nr_atoms = 3
}
atom_type;


#define temperature	298.0

#define boltz		1.380662e-16
#define unit_t		1.0e-15
#define unit_l		1.0e-8
#define unit_m		1.6605655e-24

#define h_mass		1.007825
#define o_mass 		15.99945
#define h2o_mass	(2.0 * h_mass + o_mass)

#define rho		0.9980

#define roh		0.9572
#define rohi		(1.0 / roh)
#define rohi2		(rohi * rohi)

#define angle		1.824218

#define min	-0.4
#define max	 0.4

double vel [max_molecules] [nr_dirs] [nr_atoms];


double random_value(void)
{
    double val1 = (double) rand();
    double val2 = (double) rand();
    val1 /= pow(2.0, 15.0);
    val2 /= pow(2.0, 30.0);
    return min + (max - min) * (val1 + val2);
}


int main(int argc, char **argv)
{
    FILE	*out = stdout;
    double	timestep_length, box_length, distance, range, wsin, wcos, xt[3], yt[3], z;
    int	   	nr_timesteps, nr_mols, n_order, n_print, mols_per_side, i, j, k, dir, atom, mol;

    if (argc != 6)
    {
	fprintf(stderr, "Usage: %s timestep_length nr_timesteps nr_mols n_order n_print\n", argv [0]);
	exit(1);
    }

    timestep_length = atof(argv [1]);
    nr_timesteps    = atoi(argv [2]);
    nr_mols	    = atoi(argv [3]);
    n_order	    = atoi(argv [4]);
    n_print	    = atoi(argv [5]);

    if (nr_mols > max_molecules)
    {
	fprintf(stderr, "max_molecules = %u\n", max_molecules);
	exit(1);
    }

    fprintf(out, "%g\n%u\n%u\n%u\n-1\n%u\n0.0\n", timestep_length, nr_timesteps, nr_mols, n_order, n_print);

    box_length	  = pow(nr_mols * h2o_mass * unit_m / rho, 1.0 / 3.0) / unit_l;
    mols_per_side = ceil( pow((double) nr_mols, 1.0 / 3.0));
    distance	  = box_length / mols_per_side;
    range	  = distance / 2;
    wcos	  = roh * cos(angle / 2);
    wsin	  = roh * sin(angle / 2);
    xt [1]	  = range;

    for (mol = 0; mol < nr_mols; mol ++)
	for (dir = 0; dir < nr_dirs; dir ++)
	    for (atom = 0; atom < nr_atoms; atom ++)
		vel[mol][dir][atom] = random_value();

    for (mol = 0; mol < nr_mols; mol++) {
	for (atom = 0; atom < nr_atoms; atom ++) {
	    for (dir = 0; dir < nr_dirs; dir ++) {
		printf("%3.18g ", vel [mol] [dir] [atom]);
	    }
	    printf("\n");
	}
    }

    return 0;
}
