import java.io.*;

strictfp public final class Vec3 implements Serializable {

	//public static int InstanceCount = 0;

	public double x, y, z;

	public Vec3() {
		x = 0.0;
		y = 0.0;
		z = 0.0;

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
		String xstr = new Double(x).toString().substring(0,6);
		String ystr = new Double(y).toString().substring(0,6);
		String zstr = new Double(z).toString().substring(0,6);
		return "(" + xstr + ", " + ystr + ", " + zstr + ")";
	}
}
