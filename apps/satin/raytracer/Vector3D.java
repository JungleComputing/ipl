
class Vector3D implements java.io.Serializable 
{
  double x, y, z;
  double len;

  Vector3D(double r, 
	  double g, 
	  double b)
    {
      this.x = r;
      this.y = g;
      this.z = b;
    }
  Vector3D(Vector3D v)
    {
      x = v.x;
      y = v.y;
      z = v.z;
    }

  double GetZXAngle()
    {
      return (double) Math.atan2(z, x);
    }

  double GetZYAngle()
    {
      return (double) Math.atan2(z, y);
    }

  double GetXYAngle()
    {
      return (double) Math.atan2(y, x);
    }
      
public String toString()
    {
      return "vx:"+ x +", vy: " + y +", vz: " + z;
    }

  void Print()
    {
      System.out.println("vx:"+ x +", vy: " + y +", vz: " + z);
    }
  
  double length()
    {
      return (double) Math.sqrt((x*x)+(y*y)+(z*z));
    }
  Vector3D Invert()
    {
      x = - x;
      y = - y;
      z = - z;
      return this;
    }

  static Vector3D ReSize(Vector3D vec, double a)
    {
      return new Vector3D(vec.x * a,
			  vec.y * a,
			  vec.z * a);
    }

  void InverseScale(double a)
    {
      x = x / a;
      y = y / a;
      z = z / a;
    }

  double normalize()
    {
      double len = length();
      x /= len;
      y /= len;
      z /= len;
      return len;
    }
  static double DotProduct(Vector3D v1,
			  Vector3D v2)
    {      
      return 
	v1.x * v2.x +
	v1.y * v2.y +
	v1.z * v2.z;
    }
  static Vector3D Interpolate(Vector3D v1, 
			      Vector3D v2,
			      double w1,
			      double w2)
  {
      return new Vector3D((w1 * v1.x) + (w2 * v2.x),
			  (w1 * v1.y) + (w2 * v2.y),
			  (w1 * v1.z) + (w2 * v2.z));
    }

  static Vector3D Substract(Vector3D v1,
			 Vector3D v2)
    {      
      return new Vector3D(v1.x - v2.x,
			  v1.y - v2.y,
			  v1.z - v2.z);
    }

  static Vector3D Multiply(Vector3D v1,
			   Vector3D v2)
    {      
      return new Vector3D(v1.x * v2.x,
			  v1.y * v2.y,
			  v1.z * v2.z);
    }
  static Vector3D Add(Vector3D v1,
		      Vector3D v2)
    {      
      return new Vector3D(v1.x + v2.x,
			  v1.y + v2.y,
			  v1.z + v2.z);
    }
  static Vector3D CrossProduct(Vector3D v1,
			       Vector3D v2)
    {
      return new Vector3D((v1.y*v2.z) - (v1.z*v2.y),
			  (v1.z*v2.x) - (v1.x*v2.z),
			  (v1.x*v2.y) - (v1.y*v2.x));
    }
  void RotateZX(double angle)
    {
      double c = (double) Math.cos(angle);
      double s = (double) Math.sin(angle);
      double new_x = (x * c) - (s * z);
      z = (x * s) + (c * z);
      x = new_x;
    }

  void RotateZY(double angle)
    {
      double c = (double) Math.cos(angle);
      double s = (double) Math.sin(angle);
      double new_y = (y * c) - (s * z);
      z = (y * s) + (c * z);
      y = new_y;
    }

  void RotateXY(double angle)
    {
      double c = (double) Math.cos(angle);
      double s = (double) Math.sin(angle);
      double new_y = (x * c) - (s * y);
      x = (x * s) + (c * y);
      y = new_y;
    }
}










