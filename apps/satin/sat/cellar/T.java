// File: $Id$
//
// Minimal Satin program.

package cellar;


// Result exception.
class TX extends Exception {
    int n;

    TX( int n ){ this.n = n; }
}

interface TI extends ibis.satin.Spawnable
{
    int run( int low, int high ) throws TX;
}

public class T extends ibis.satin.SatinObject implements TI, java.io.Serializable {
    static final boolean trace = true;
    static int target = 42;

    public int run( int low, int high ) throws TX
    {
	if( trace ){
	    System.err.println( "Run with low=" + low + " high=" + high );
	}
	if( low == high ){
	    if( low == target ){
	        throw new TX( low );
	    }
	    return 1;
	}
	int mid = (low+high)/2;
	int va = run( low, mid );
	int vb = run( mid+1, high );
	sync();
	return 1+va+vb;
    }

    public static void main( String args[] ) throws java.io.IOException
    {
	long startTime = System.currentTimeMillis();
	int n = -1;

	T s = new T();
	try {
	    n = s.run( 0, 128 );
	    s.sync();
	}
	catch( TX x ){
	    System.out.println( "Caught result exception: " + x.n );
	}
	long endTime = System.currentTimeMillis();

	System.out.println( "Result: " + n );

	double time = ((double) (endTime - startTime))/1000.0;

	System.out.println( "Time: " + time );
    }
}
