/* $Id$ */


class Cone3D extends Object3D {
    Vector3D p1, p2;

    float r1, r2;

    Cone3D(World world, Surface c, Vector3D p1, float r1, Vector3D p2, float r2) {
        super(world, c);

        this.p1 = p1;
        this.p2 = p2;
        this.r1 = r1;
        this.r2 = r2;
    }

    boolean Intersect(Ray3D Ray) {

        return false;
    }

}