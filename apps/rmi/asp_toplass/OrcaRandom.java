/*
# Implementation in orca of Numerical Recipes ran1() but with integer result
# in the range 0 .. 2^31 -1.
# Assume 32 bit signed integers: 1/ otherwise the 3 simple random generators
#				    overflow (or have a too small range);
#				 2/ the combination of higher/lower bits is
#				    wrong.
*/

public class OrcaRandom {

    static final int table_size = 97;
    static final int two_16     = 65536;
    static final int two_15     = two_16 / 2;
    static final double two_31	    = (double)two_16 * (double)two_15;

    int[] term = {  54773,  28411,  51349};
    int[] fac =  {   7141,   8121,   4561};
    int[] mod =  { 259200, 134456, 243000};

    double[] inv_mod = new double[3];
    int[] current = new int[3];
    double[] table = new double[table_size];


    public int val() {
	return (int)(val01() * two_31);
    }


    public double val01() {
	int ix;
	double r;

	current[0] = (fac[0] * current[0] + term[0]) % mod[0];
	current[1] = (fac[1] * current[1] + term[1]) % mod[1];
	current[2] = (fac[2] * current[2] + term[2]) % mod[2];
	ix = (table_size * current[2]) / mod[2];
	r  = table[ix];
	table[ix] = ((double)current[0] + (double)current[1] * inv_mod[1]) * inv_mod[0];
	return r;
    }


    int nextInt() {
	return val();
    }

    public OrcaRandom() {
	this(1);
    }

    public OrcaRandom(int seed) {
	if(seed <= 0) {
	    System.out.println("Seed must be greater than 0");
	    System.exit(1);
	}

	for(int i=0; i<3; i++) {
	    inv_mod[i] = 1.0 / (double)mod[i];
	}

	current[0] = (term[0] + seed) % mod[0];
	current[0] = (fac[0] * current[0] + term[0]) % mod[0];
	current[1] = current[0] % mod[1];
	current[0] = (fac[0] * current[0] + term[0]) % mod[0];
	current[2] = current[0] % mod[2];

	for(int i=0; i<table_size; i++) {
	    current[0] = (fac[0] * current[0] + term[0]) % mod[0];
	    current[1] = (fac[1] * current[1] + term[1]) % mod[1];
	    table[i]   = ((double)current[0] + (double)current[1] * inv_mod[1]) * inv_mod[0];
	}
    }
}

