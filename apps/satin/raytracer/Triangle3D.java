
class Triangle3D extends Polygon3D {
    static Vector3D[] CreateArray(Vector3D p1, Vector3D p2, Vector3D p3) {
        Vector3D[] v = new Vector3D[3];
        v[0] = p1;
        v[1] = p2;
        v[2] = p3;
        return v;
    }

    Triangle3D(World world, Vector3D p1, Vector3D p2, Vector3D p3, Surface color) {
        super(world, color, 3, CreateArray(p1, p2, p3));
    }
}

