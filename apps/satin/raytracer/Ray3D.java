/* $Id$ */

class Ray3D implements java.io.Serializable {
    Vector3D pos;

    Vector3D vec;

    GfxColor color;

    double min_dist = World.MAX_DIST;

    Object3D closest;

    Ray3D() {
        this(new Vector3D(0, 0, 0), new Vector3D(0, 0, 1));
    }

    Ray3D(Vector3D pos, Vector3D vec) {
        color = new GfxColor(0f, 0f, 0f);
        this.vec = vec;
        this.pos = pos;
    }

    Ray3D(Vector3D pos, Vector3D vec, GfxColor c) {
        color = c;
        this.vec = vec;
        this.pos = pos;
    }

    Vector3D Advance(double t) {
        return new Vector3D(pos.x + (t * vec.x), pos.y + (t * vec.y), pos.z
                + (t * vec.z));
    }

    void RotateZX() {
    }
}