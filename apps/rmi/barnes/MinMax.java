/* $Id$ */

final public class MinMax implements java.io.Serializable {
    vec3 min, max;

    MinMax() {
        min = new vec3();
        max = new vec3();
        min.x = Double.MAX_VALUE;
        min.y = Double.MAX_VALUE;
        min.z = Double.MAX_VALUE;
        max.x = -Double.MAX_VALUE;
        max.y = -Double.MAX_VALUE;
        max.z = -Double.MAX_VALUE;
    }
}