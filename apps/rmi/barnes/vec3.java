import java.lang.*;
import java.io.*;

strictfp public final class vec3 implements Serializable {

  public static int InstanceCount = 0;

  public double x, y, z;

  public vec3() {

    x = 0.0;
    y = 0.0;
    z = 0.0;

    InstanceCount++;
  }

  public vec3( double _x, double _y, double _z ) {

    x = _x;
    y = _y;
    z = _z;

    InstanceCount++;
  }

  void add( vec3 v ) {

    x += v.x;
    y += v.y;
    z += v.z;
  }

  void add( double d ) {

    x += d;
    y += d;
    z += d;
  }

  void mul( vec3 v ) {

    x *= v.x;
    y *= v.y;
    z *= v.z;
  }

  void mul( double d ) {

    x *= d;
    y *= d;
    z *= d;
  }

  void div( double d ) {

    if (d!=0.0) {

      double recip = 1.0/d;

      x *= recip;
      y *= recip;
      z *= recip;
    }
    // TODO: some error handling here
  }

  double element( int i ) {
    switch (i) {
      case 0: return x;
      case 1: return y;
      case 2: return z;
    }
    return 0;
  }
  
  void setElement( int i, double value ) {
    switch (i) {
      case 0: x = value; break;
      case 1: y = value; break;
      case 2: z = value; break;
    }
  }

}
