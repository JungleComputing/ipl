/* $Id$ */


import java.io.Serializable;

public final class SendBody implements Serializable, Cloneable {

    public vec3 bPos;

    public double bMass;

    SendBody() {
        bPos = new vec3();
    }
}