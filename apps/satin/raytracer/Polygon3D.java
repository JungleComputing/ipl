class Polygon3D extends Plane3D {
    int pts;

    Vector3D[] points;

    Polygon3D(World world, Surface surf, int pts, Vector3D[] p) {
        super(world, Vector3D.CrossProduct(Vector3D.Substract(p[2], p[0]),
                Vector3D.Substract(p[1], p[0])), p[0], surf);

        this.pts = pts;
        this.points = p;
    }

    boolean PointInPoly(Vector3D P) {
        if (major == 0) {
            return PointInPoly_XY(P);
        } else {
            return PointInPoly_ZY(P);
        }
    }

    boolean PointInPoly_ZY(Vector3D P) {

        double u0 = P.z - points[0].z;
        double v0 = P.y - points[0].y;

        boolean inter = false;

        double alpha, beta;
        int i = 1;

        do {

            double u1 = points[i].z - points[0].z;
            double v1 = points[i].y - points[0].y;

            double u2 = points[i + 1].z - points[0].z;
            double v2 = points[i + 1].y - points[0].y;

            if (u1 == 0) {
                beta = u0 / u2;
                if ((beta >= 0.) && (beta <= 1.)) {
                    alpha = (v0 - beta * v2) / v1;
                    inter = ((alpha >= 0.) && (alpha + beta) <= 1.0f);
                }
            } else {
                beta = (v0 * u1 - u0 * v1) / (v2 * u1 - u2 * v1);
                if ((beta >= 0.) && (beta <= 1.)) {
                    alpha = (u0 - beta * u2) / u1;
                    inter = ((alpha >= 0) && ((alpha + beta) <= 1.0f));
                }
            }
        } while ((!inter) && (++i < pts - 1));

        return inter;
    }

    boolean PointInPoly_XY(Vector3D P) {

        double u0 = P.x - points[0].x;
        double v0 = P.y - points[0].y;

        boolean inter = false;

        double alpha, beta;
        int i = 1;

        do {

            double u1 = points[i].x - points[0].x;
            double v1 = points[i].y - points[0].y;

            double u2 = points[i + 1].x - points[0].x;
            double v2 = points[i + 1].y - points[0].y;

            if (u1 == 0) {
                beta = u0 / u2;
                if ((beta >= 0.) && (beta <= 1.)) {
                    alpha = (v0 - beta * v2) / v1;
                    inter = ((alpha >= 0.) && (alpha + beta) <= 1.0f);
                }
            } else {
                beta = (v0 * u1 - u0 * v1) / (v2 * u1 - u2 * v1);
                if ((beta >= 0.) && (beta <= 1.)) {
                    alpha = (u0 - beta * u2) / u1;
                    inter = ((alpha >= 0) && ((alpha + beta) <= 1.0f));
                }
            }
        } while ((!inter) && (++i < pts - 1));

        return inter;
    }

    boolean Intersect(Ray3D ray) {
        Vector3D vec = ray.vec;
        Vector3D pos = ray.pos;

        Vector3D P = Vector3D.Substract(new Vector3D(pos), origin);

        double normalXvec = normal.x * vec.x + normal.y * vec.y + normal.z
                * vec.z;

        double normalXpos = normal.x * P.x + normal.y * P.y + normal.z * P.z;

        double dist = -(normalXpos + plane_distance) / normalXvec;

        if (dist > 0.000001 && dist < ray.min_dist) {

            Vector3D p = new Vector3D((dist * ray.vec.x) + ray.pos.x,
                    (dist * ray.vec.y) + ray.pos.y, (dist * ray.vec.z)
                            + ray.pos.z);
            if (PointInPoly(p)) {
                ray.min_dist = dist;
                ray.closest = this;
                return true;
            }
        }
        return false;
    }

    GfxColor GetColor(Vector3D pos, Vector3D vec, int depth) {
        GfxColor light = world.GetLight(pos, normalized_normal, depth);

        double angle = Math.abs(Vector3D.DotProduct(vec, normalized_normal)
                * surface.angle_dep);

        if (angle > 1)
            angle = 1;

        GfxColor c = GfxColor.Interpolate(surface.color, world.background,
                (float) (surface.diffusity * angle));

        c = c.Multiply(light).DirectMultiply((float) angle);

        GfxColor reflected_color = world.Reflect(normalized_normal, vec, pos,
                depth);
        if (reflected_color == null)
            reflected_color = world.background;

        c.r = ((c.r * surface.diffusity) + (reflected_color.r * surface.reflection))
                / surface.absorbtion;

        c.g = ((c.g * surface.diffusity) + (reflected_color.g * surface.reflection))
                / surface.absorbtion;

        c.b = ((c.b * surface.diffusity) + (reflected_color.b * surface.reflection))
                / surface.absorbtion;

        c.DirectMultiply(1 / (depth));

        c.UpperClip();

        return c;
    }
}