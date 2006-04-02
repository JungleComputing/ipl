/* $Id$ */

/**
 * Container for collecting accellerations. This container is used for
 * job results, as well as for sending updates in the SO version.
 */
public final class BodyUpdates implements java.io.Serializable {
    /** Body number corresponding to the index. */
    private int[] bodyNumbers;

    /** Acceleration in X direction. */
    double[] acc_x;

    /** Acceleration in Y direction. */
    double[] acc_y;

    /** Acceleration in Z direction. */
    double[] acc_z;

    /** Current fill index. */
    private int index;

    /** For combining BodyUpdate containers. */
    private BodyUpdates[] more;

    /**
     * Constructor.
     * @param sz the initial size of the accelleration arrays.
     */
    public BodyUpdates(int sz) {
        bodyNumbers = new int[sz];
        acc_x = new double[sz];
        acc_y = new double[sz];
        acc_z = new double[sz];
        index = 0;
    }

    /**
     * Grow (or shrink) to the specified size.
     * @param newsz the size to grow or shrink to.
     */
    private void grow(int newsz) {
        if (newsz != bodyNumbers.length) {
            int[] newnums = new int[newsz];
            System.arraycopy(bodyNumbers, 0, newnums, 0, index);
            bodyNumbers = newnums;
            double[] newacc = new double[newsz];
            System.arraycopy(acc_x, 0, newacc, 0, index);
            acc_x = newacc;
            newacc = new double[newsz];
            System.arraycopy(acc_y, 0, newacc, 0, index);
            acc_y = newacc;
            newacc = new double[newsz];
            System.arraycopy(acc_z, 0, newacc, 0, index);
            acc_z = newacc;
        }
    }

    /**
     * Adds the specified accelerations for the specified body number.
     * @param bodyno the body number.
     * @param x the acceleration in the X direction.
     * @param y the acceleration in the Y direction.
     * @param z the acceleration in the Z direction.
     */
    public void addData(int bodyno, double x, double y, double z) {
        if (index >= bodyNumbers.length) {
            System.out.println("Should not happen 1");
            grow(2*index+1);
        }
        bodyNumbers[index] = bodyno;
        acc_x[index] = x;
        acc_y[index] = y;
        acc_z[index] = z;
        index++;
    }

    /**
     * Allocates and returns an index for the specified body number.
     * @param bodyno the body number
     * @return the corresponding index in this BodyUpdates structure,
     */
    public int updatePos(int bodyno) {
        if (index >= bodyNumbers.length) {
            System.out.println("Should not happen 2");
            grow(2*index+1);
        }
        bodyNumbers[index] = bodyno;
        return index++;
    }

    /**
     * Computes the number of bodies for which this BodyUpdates structure
     * (and its nested structures) has updates.
     * @return the number of updates.
     */
    private int computeSz() {
        int sz = index;
        if (more != null) {
            for (int i = 0; i < more.length; i++) {
                sz += more[i].computeSz();
            }
        }
        return sz;
    }

    /**
     * Optimizes this BodyUpdates structure for sending, by collecting
     * its nested structures into the current one.
     */
    private void optimizeAndTrim() {
        if (bodyNumbers != null) {
            if (more != null || index < bodyNumbers.length) {
                int newsz = computeSz();
                grow(newsz);
                if (more != null) {
                    for (int i = 0; i < more.length; i++) {
                        optimize(more[i]);
                    }
                    more = null;
                }
            }
        }
    }

    /**
     * Collects the specified updates into the current one.
     * @param b the updates to be added.
     */
    private void optimize(BodyUpdates b) {
        System.arraycopy(b.bodyNumbers, 0, bodyNumbers, index, b.index);
        System.arraycopy(b.acc_x, 0, acc_x, index, b.index);
        System.arraycopy(b.acc_y, 0, acc_y, index, b.index);
        System.arraycopy(b.acc_z, 0, acc_z, index, b.index);
        index += b.index;
        if (b.more != null) {
            for (int i = 0; i < b.more.length; i++) {
                optimize(b.more[i]);
            }
        }
    }

    /**
     * Object output serialization. Optimizes the object and then sends it
     * out using the default write method.
     * @param out the stream to write to.
     */
    private void writeObject(java.io.ObjectOutputStream out)
            throws java.io.IOException {
        optimizeAndTrim();
        out.defaultWriteObject();
    }

    /**
     * Combines the specified updates into this one and returns the result.
     * Assumes that the current BodyUpdates has no <code>more</code> array
     * yet.
     * @param v the updates to combine into this one.
     * @return the result.
     */
    public BodyUpdates combineResults(BodyUpdates[] v) {
        if (more != null) {
            throw new Error("Oops: something wrong here.");
        }
        more = v;
        return this;
    }

    /**
     * Adds the specified updates, preparing for an update round.
     * @param r the specified updates.
     */
    private void addUpdates(BodyUpdates r) {
        for (int i = 0; i < r.index; i++) {
            int ix = r.bodyNumbers[i];
            acc_x[ix] = r.acc_x[i];
            acc_y[ix] = r.acc_y[i];
            acc_z[ix] = r.acc_z[i];
        }
        if (r.more != null) {
            for (int i = 0; i < r.more.length; i++) {
                addUpdates(r.more[i]);
            }
            r.more = null;
        }
    }

    /**
     * Prepares for an update round. It changes the order in the acceleration
     * arrays to the body order, and removes the bodyNumbers array, as it is
     * no longer needed, and this saves on serialization and sending time
     * when the BodyUpdate gets broadcasted.
     */
    public void prepareForUpdate() {
        int sz = computeSz();
        double[] ax = new double[sz];
        double[] ay = new double[sz];
        double[] az = new double[sz];
        for (int i = 0; i < index; i++) {
            int ix = bodyNumbers[i];
            ax[ix] = acc_x[i];
            ay[ix] = acc_y[i];
            az[ix] = acc_z[i];
        }
        bodyNumbers = null;
        acc_x = ax;
        acc_y = ay;
        acc_z = az;
        if (more != null) {
            for (int i = 0; i < more.length; i++) {
                addUpdates(more[i]);
            }
            more = null;
        }
    }

    /**
     * Applies the updates to the bodies in the specified array.
     * @param bodyArray the bodies
     * @param iteration the current iteration number
     * @param params the run parameters.
     */
    public void updateBodies(Body[] bodyArray, int iteration,
            RunParameters params) {
        for (int i = 0; i < bodyArray.length; i++) {
            bodyArray[i].computeNewPosition(iteration != 0,
                    acc_x[i], acc_y[i], acc_z[i], params);
        }
    }
}
