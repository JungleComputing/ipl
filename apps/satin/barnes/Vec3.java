import java.io.*;

strictfp public final class Vec3 implements Serializable, Comparable {

    //public static int InstanceCount = 0;

    public double x, y, z;

    /**
     * The default contructor initializes x, y, and z to 0.0
     * Watch out! Several classes depend on x, y, and z being 0.0!
     */
    public Vec3() {
	x = y = z = 0.0;
	//InstanceCount++;
    }

    public Vec3( double _x, double _y, double _z ) {
	x = _x;
	y = _y;
	z = _z;

	//InstanceCount++;
    }

    public Vec3(Vec3 v) {
	x = v.x;
	y = v.y;
	z = v.z;
    }

    public void add( Vec3 v ) {
	x += v.x;
	y += v.y;
	z += v.z;
    }

    public void add( double d ) {
	x += d;
	y += d;
	z += d;
    }

    public void sub( Vec3 v ) {
	x -= v.x;
	y -= v.y;
	z -= v.z;
    }

    public void sub( double d ) {
	x -= d;
	y -= d;
	z -= d;
    }

    public void mul( Vec3 v ) {
	x *= v.x;
	y *= v.y;
	z *= v.z;
    }

    public void mul( double d ) {
	x *= d;
	y *= d;
	z *= d;
    }

    public void div( double d ) {
	if (d!=0.0) {
	    double recip = 1.0/d;

	    x *= recip;
	    y *= recip;
	    z *= recip;
	} else {
	    System.err.println("Division by zero in Vec3.java!");
	    // TODO: some error handling here
	}
    }

    /**
     * x, y, z become the maximum of the values of x, y, z in this and v
     */
    public void max( Vec3 v ) {
	x = Math.max(x, v.x);
	y = Math.max(y, v.y);
	z = Math.max(z, v.z);
    }

    /**
     * x, y, z become the minimum of the values of x, y, z in this and v
     */
    public void min( Vec3 v ) {
	x = Math.min(x, v.x);
	y = Math.min(y, v.y);
	z = Math.min(z, v.z);
    }

    /* These don't seem very useful if x, y, and z are public
       public double element( int i ) {
       switch (i) {
       case 0:
       return x;
       case 1:
       return y;
       case 2:
       return z;
       default:
       System.err.println("Invalid argument in Vec3.element()!");
       System.exit(1);
       return 0.0;
       }
       }
  
       void setElement( int i, double value ) {
       switch (i) {
       case 0: x = value; break;
       case 1: y = value; break;
       case 2: z = value; break;
       default:
       System.err.println("Invalid argument in Vec3.setElement()!");
       System.exit(1);
       }
       }
    */

    public String toString() {
	String xstr = new Double(x).toString();
	String ystr = new Double(y).toString();
	String zstr = new Double(z).toString();
	int xlen = Math.min(xstr.length(), 6);
	int ylen = Math.min(ystr.length(), 6);
	int zlen = Math.min(zstr.length(), 6);
	return "(" + xstr.substring(0, xlen) + ", " +
	    ystr.substring(0, ylen) + ", " + zstr.substring(0, zlen) + ")";
    }

    public boolean equals(Vec3 other) {
	return ((x == other.x) && (y == other.y) && (z == other.z));
    }

    public int compareTo(Object o) {
	Vec3 other = (Vec3) o;
	if (x < other.x) return -1;
	if (x > other.x) return 1;
	if (y < other.y) return -1;
	if (y > other.y) return 1;
	if (z < other.z) return -1;
	if (z > other.z) return 1;
	/* else */
	return 0;
    }		
}
