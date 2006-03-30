/* $Id$ */

import java.io.*;

strictfp public final class Body implements Cloneable, Comparable, Serializable {

    public int number;

    public double pos_x, pos_y, pos_z;

    public double mass;

    // these are only used by calculateNewPosition, which is done at the
    // main node, so they can be transient
    transient public double vel_x, vel_y, vel_z;

    transient public double oldAcc_x, oldAcc_y, oldAcc_z;

    transient public boolean updated = false; //used for debugging

    void initialize() {
        mass = 1.0;
        number = 0;
    }

    Body() {
        initialize();
    }

    //used for sorting bodies, they're sorted using the 'number' field
    public int compareTo(Object o) {
        Body other = (Body) o;

        return this.number - other.number;
    }

    //copied from the rmi implementation
    //I used 'newAcc' instead of 'acc' to avoid confusion with this.acc
    public void computeNewPosition(boolean useOldAcc,
            double newAcc_x, double newAcc_y, double newAcc_z, RunParameters params) {
        if (useOldAcc) { // always true, except for first iteration
            vel_x += (newAcc_x - oldAcc_x) * params.DT_HALF;
            vel_y += (newAcc_y - oldAcc_y) * params.DT_HALF;
            vel_z += (newAcc_z - oldAcc_z) * params.DT_HALF;
        }

        pos_x += (newAcc_x * params.DT_HALF + vel_x) * params.DT;
        pos_y += (newAcc_y * params.DT_HALF + vel_y) * params.DT;
        pos_z += (newAcc_z * params.DT_HALF + vel_z) * params.DT;

        vel_x += newAcc_x * params.DT;
        vel_y += newAcc_y * params.DT;
        vel_z += newAcc_z * params.DT;

        //prepare for next call of BodyTreeNode.barnes()
        oldAcc_x = newAcc_x;
        oldAcc_y = newAcc_y;
        oldAcc_z = newAcc_z;
    }

    public String toString() {
        return "pos: (" + pos_x + ", " + pos_y + ", " + pos_z + "), vel: ("
            + vel_x + ", " + vel_y + ", " + vel_z + ")";
    }
}
