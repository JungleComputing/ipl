/* $Id$ */


import java.io.Serializable;

public final class Body implements Cloneable, Comparable, Serializable {

    public int number;

    public Vec3 pos;

    public double mass;

    // these are only used by calculateNewPosition, which is done at the
    // main node, so they can be transient
    transient public Vec3 vel;

    transient public Vec3 oldAcc;

    transient public Vec3 acc;

    transient public boolean updated = false; //used for debugging

    void initialize() {
        //oldAcc = acc = null;
        mass = 1.0;

        number = 0;
    }

    Body() {
        pos = new Vec3();
        vel = new Vec3();
        initialize();
    }

    Body(double x, double y, double z) {
        pos = new Vec3(x, y, z);
        vel = new Vec3();
        initialize();
    }

    Body(double x, double y, double z, double vx, double vy, double vz) {
        pos = new Vec3(x, y, z);
        vel = new Vec3(vx, vy, vz);
        initialize();
    }

    //used for sorting bodies, they're sorted using the 'number' field
    public int compareTo(Object o) {
        Body other = (Body) o;

        return this.number - other.number;
    }

    //copied from the rmi implementation
    public void computeNewPosition(boolean useOldAcc, double dt, Vec3 acc) {
        Vec3 v;

        if (useOldAcc) {
            v = new Vec3(acc); //vel += (acc-oldacc) * DT_HALF
            v.sub(oldAcc);
            v.mul(dt / 2.0);
            vel.add(v);
        }

        v = new Vec3(acc); //pos += (acc * DT_HALF + vel) * DT
        v.mul(dt / 2.0);
        v.add(vel);
        v.mul(dt);
        pos.add(v);

        v = new Vec3(acc); //vel += acc * DT
        v.mul(dt);
        vel.add(v);

        //prepare for next call of BodyTreeNode.barnes()
        oldAcc = acc;
    }
}