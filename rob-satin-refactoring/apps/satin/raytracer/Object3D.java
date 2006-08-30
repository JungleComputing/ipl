/* $Id$ */

class Object3D implements java.io.Serializable {
    Object3D Next;

    Surface surface = new Surface();

    World world;

    Object3D(World w, Surface surf) {
        surface = surf.copy();
        world = w;
    }

    boolean Intersect(Ray3D ray) {
        System.out.println("we still have something to do...\n");
        return false;
    }

    GfxColor GetColor(Vector3D p, Vector3D vec, int depth) {
        return surface.color;
    }

    void RotateZX(Vector3D axis, double a) {
    }

    public String toString() {
        String res = "Object3D";
        if (Next != null) {
            res += " " + Next.toString();
        }
        return res;
    }

    public String getString() {
        String res = this.toString();
        if (Next != null) {
            res += " " + Next.getString();
        }
        return res;
    }

}