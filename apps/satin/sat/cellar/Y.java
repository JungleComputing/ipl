// File: $Id$
//
// Minimal Satin program.

package cellar;


// Result exception.
class YX extends Exception {
    int n;

    YX( int n ){ this.n = n; }
}

interface YI extends ibis.satin.Spawnable
{
    void run( int low, int high ) throws YX;
}

public class Y extends ibis.satin.SatinObject implements YI, java.io.Serializable {
    static final boolean trace = true;
    static int target = 42;

    public void run( int low, int high ) throws YX
    {
	if( trace ){
	    System.err.println( "Run with low=" + low + " high=" + high );
	}
	if( low == high ){
	    if( low == target ){
	        throw new YX( low );
	    }
	    return;
	}
	int mid = (low+high)/2;
	run( low, mid );
	run( mid+1, high );
	sync();
    }

    public static void main( String args[] ) throws java.io.IOException
    {
	long startTime = System.currentTimeMillis();

	Y s = new Y();
	try {
	    s.run( 0, 128 );
	    s.sync();
	}
	catch( YX x ){
	    System.out.println( "Caught result exception: " + x.n );
	}
	long endTime = System.currentTimeMillis();

	double time = ((double) (endTime - startTime))/1000.0;

	System.out.println( "Time: " + time );
    }
}
