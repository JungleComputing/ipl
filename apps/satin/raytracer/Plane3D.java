/* $Id$ */


class Plane3D extends Object3D {
    Vector3D normal, origin, normalized_normal;

    double plane_distance;

    double sx = 2f;

    double sy = 2f;

    int major = 0;

    Plane3D(World world, Vector3D normal, Vector3D origin, Surface color) {
        super(world, color);

        normalized_normal = new Vector3D(normal);
        normalized_normal.normalize();

        this.normal = normal;
        this.origin = origin;

        plane_distance = origin.x + origin.y + origin.z;

        major = XY_Triangle_Surface() ? 0 : 1;
    }

    boolean XY_Triangle_Surface() {
        if (normal.z > normal.x)
            return false;
        return true;
    }

    GfxColor GetColor(Vector3D pos, Vector3D vec, int depth) {
        GfxColor light = world.GetLight(pos, normal, depth);

        double angle = Vector3D.DotProduct(vec, normal);
        angle = Math.abs(angle);

        if (angle > 1)
            angle = 1;

        GfxColor c = GfxColor.Interpolate(surface.color, world.background,
                (float) (surface.diffusity * angle));

        GfxColor reflected_color = world.Reflect(normal, vec, pos, depth);
        if (reflected_color != null) {
            c.Multiply(reflected_color);
        }
        return c;
    }

    boolean Intersect(Ray3D ray) {
        Vector3D vec = ray.vec;
        Vector3D pos = ray.pos;

        Vector3D P = Vector3D.Substract(new Vector3D(pos), origin);

        double normalXvec = normal.x * vec.x + normal.y * vec.y + normal.z
                * vec.z;

        double normalXpos = normal.x * pos.x + normal.y * pos.y + normal.z
                * pos.z;

        double dist = -(normalXpos + plane_distance) / normalXvec;

        if (dist > 0.000001 && dist < ray.min_dist) {
            ray.min_dist = dist;
            ray.closest = this;
            return true;
        }
        return false;
    }
}

