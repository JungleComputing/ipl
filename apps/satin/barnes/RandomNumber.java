import java.lang.*;

class RandomNumber {

    private static final int rnMult = 1103515245;
    private static final int rnAdd  = 12345;
    private static final int rnMask = 0x7fffffff;
    private static final double rnTwoTo31 = 2147483648.0;

    private int rnA;
    private int rnB;
    private int rnRandX;
    private int rnLastRand;
    private int rnSeed;
  

    RandomNumber() {
	setSeed( 123 );
    }


    void setSeed( int Seed ) {

	rnA = 1;
	rnB = 0;
	rnRandX = (( rnA * Seed + rnB  ) & rnMask );
	rnA = (( rnMult * rnA ) & rnMask );
	rnB = (( rnMult * rnB + rnAdd ) & rnMask );
    }
  

    double pRand() {

	rnLastRand = rnRandX;
	rnRandX = (( rnA * rnRandX + rnB ) & rnMask );   

	return (( double )( rnLastRand ) / rnTwoTo31 );
    }


    double xRand( double Low, double High ) {

	return ( Low + ( High - Low ) * pRand() );
    }


    void pickShell( Vec3 point, double Radius ) {

	double rsq, rsc;

	do {

	    point.x = xRand( -1.0, 1.0 );
	    point.y = xRand( -1.0, 1.0 );
	    point.z = xRand( -1.0, 1.0 );

	    // System.out.println("x = " + point.x + ", y = " + point.y + ", z = " + point.z);

	    rsq = point.x * point.x + point.y * point.y + point.z * point.z;

	} while ( rsq > 1.0 );
    
	rsc = Radius / Math.sqrt( rsq );

	point.x *= rsc;
	point.y *= rsc;
	point.z *= rsc;
    }



}
