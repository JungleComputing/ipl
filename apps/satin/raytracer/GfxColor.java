/* $Id$ */

public class GfxColor implements java.io.Serializable {
    float r, g, b;

    GfxColor(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    GfxColor(GfxColor c) {
        this.r = c.r;
        this.g = c.g;
        this.b = c.b;
    }

    GfxColor Add(GfxColor c) {
        r += c.r;
        g += c.g;
        b += c.b;
        return this;
    }

    GfxColor Add(float _r, float _g, float _b) {
        r += _r;
        g += _g;
        b += _b;
        return this;
    }

    public String toString() {
        return "ir" + r + ", ig" + g + ", ib" + b;
    }

    void check() {
        if (r < 0) {
            System.out.println("r<0:" + r);
            System.exit(1);
        }
        if (g < 0) {
            System.out.println("g<0:" + g);
            System.exit(1);
        }
        if (b < 0) {
            System.out.println("b<0:" + b);
            System.exit(1);
        }
        if (r > 1f) {
            System.out.println("r>1:" + r);
            System.exit(1);
        }
        if (g > 1f) {
            System.out.println("g>1:" + g);
            System.exit(1);
        }
        if (b > 1f) {
            System.out.println("b>1:" + b);
            System.exit(1);
        }
    }

    int GetRGB() {
        int ir = (int) (r * 254.0);
        int ig = (int) (g * 254.0);
        int ib = (int) (b * 254.0);

        return (ir << 16) | (ig << 8) | ib;
    }

    void checkRGB() {
        int ir = (int) (r * 254.0);
        int ig = (int) (g * 254.0);
        int ib = (int) (b * 254.0);
        //      if(ir == 19 && ig == 5 && ib == 5) {
        //	      System.out.println("COLOR is WRONG");
        //      }

    }

    static int getR(int color) {
        return (color >> 16) & 0xff;
    }

    static int getG(int color) {
        return (color >> 8) & 0xff;
    }

    static int getB(int color) {
        return color & 0xff;
    }

    /*
     Color convert()
     {
     UpperClip();
     return new Color((int)(r*254.0),
     (int)(g*254.0),
     (int)(b*254.0));
     }
     */
    GfxColor DirectMultiply(float angle) {
        r *= angle;
        g *= angle;
        b *= angle;
        return this;
    }

    GfxColor Multiply(float angle) {
        return new GfxColor(r * angle, g * angle, b * angle);
    }

    GfxColor Multiply(GfxColor light) {
        r = r * light.r;
        g = g * light.g;
        b = b * light.b;
        return this;
    }

    GfxColor UpperClip() {
        if (r > 1f)
            r = 1f;
        if (g > 1f)
            g = 1f;
        if (b > 1f)
            b = 1f;
        return this;
    }

    GfxColor LowerClip() {
        if (r < 0f)
            r = 0f;
        if (g < 0f)
            g = 0f;
        if (b < 0f)
            b = 0f;
        return this;
    }

    static GfxColor Interpolate(GfxColor c1, GfxColor c2, float ratio) {
        float f2 = 1 - ratio;
        return new GfxColor((c1.r * ratio) + (c2.r * f2), (c1.g * ratio)
                + (c2.g * f2), (c1.b * ratio) + (c2.b * f2));
    }
}