/* $Id$ */


import java.io.Serializable;

public final class Vec3 implements Serializable, Comparable {

    public double x, y, z;

    /**
     * The default contructor initializes x, y, and z to 0.0
     * Watch out! Several classes depend on x, y, and z being 0.0!
     */
    public Vec3() {
        x = y = z = 0.0;
    }

    public Vec3(double _x, double _y, double _z) {
        x = _x;
        y = _y;
        z = _z;
    }

    public Vec3(Vec3 v) {
        x = v.x;
        y = v.y;
        z = v.z;
    }

    public void add(Vec3 v) {
        x += v.x;
        y += v.y;
        z += v.z;
    }

    public void add(double d) {
        x += d;
        y += d;
        z += d;
    }

    public void sub(Vec3 v) {
        x -= v.x;
        y -= v.y;
        z -= v.z;
    }

    public void sub(double d) {
        x -= d;
        y -= d;
        z -= d;
    }

    public void mul(Vec3 v) {
        x *= v.x;
        y *= v.y;
        z *= v.z;
    }

    public void mul(double d) {
        x *= d;
        y *= d;
        z *= d;
    }

    public void div(double d) {
        x /= d;
        y /= d;
        z /= d;
    }

    /**
     * x, y, z become the maximum of the values of x, y, z in this and v
     */
    public void max(Vec3 v) {
        x = Math.max(x, v.x);
        y = Math.max(y, v.y);
        z = Math.max(z, v.z);
    }

    /**
     * x, y, z become the minimum of the values of x, y, z in this and v
     */
    public void min(Vec3 v) {
        x = Math.min(x, v.x);
        y = Math.min(y, v.y);
        z = Math.min(z, v.z);
    }

    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    public boolean equals(Vec3 other) {
        return ((x == other.x) && (y == other.y) && (z == other.z));
    }

    public int compareTo(Object o) {
        Vec3 other = (Vec3) o;
        if (x < other.x)
            return -1;
        if (x > other.x)
            return 1;
        if (y < other.y)
            return -1;
        if (y > other.y)
            return 1;
        if (z < other.z)
            return -1;
        if (z > other.z)
            return 1;
        /* else */
        return 0;
    }
}