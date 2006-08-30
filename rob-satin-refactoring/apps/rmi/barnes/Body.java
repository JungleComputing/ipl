/* $Id$ */

import java.io.Serializable;

strictfp public final class Body implements Serializable, Cloneable {

    public static int InstanceCount = 0;

    public int bNumber;

    public vec3 bPos;

    public vec3 bVel;

    public vec3 bAcc;

    public vec3 bOldAcc;

    public double bMass;

    public int bWeight;

    void Initialize() {
        bPos = new vec3();
        bVel = new vec3();
        bAcc = new vec3();
        bOldAcc = new vec3();
        bMass = 1.0;
        bNumber = 0;

        InstanceCount++;
    }

    Body() {
        Initialize();
    }
}
