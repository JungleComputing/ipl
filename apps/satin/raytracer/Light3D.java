/* $Id$ */

class Light3D extends Sphere3D implements java.io.Serializable {
    float intensity;

    Light3D NextLight;

    Light3D(World w, Vector3D center, float rad, Surface c, float intensity) {
        super(w, center, rad, c);
        this.intensity = intensity;
    }

    GfxColor GetColor(Vector3D pos, Vector3D vec, int depth) {
        return surface.color.Multiply(intensity);
    }
}