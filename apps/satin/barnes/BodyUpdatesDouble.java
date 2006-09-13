/* $Id$ */

/**
 * Container for collecting accellerations. This container is used for
 * job results, as well as for sending updates in the SO version.
 */
public final class BodyUpdatesDouble extends BodyUpdates {

    /** Cached array, to avoid re-allocation. */
    private static double[] acc_x_static;

    /** Cached array, to avoid re-allocation. */
    private static double[] acc_y_static;

    /** Cached array, to avoid re-allocation. */
    private static double[] acc_z_static;

    /** Acceleration in X direction. */
    private double[] acc_x;

    /** Acceleration in Y direction. */
    private double[] acc_y;

    /** Acceleration in Z direction. */
    private double[] acc_z;

    /**
     * Constructor.
     * @param sz the initial size of the accelleration arrays.
     */
    public BodyUpdatesDouble(int sz) {
        super(sz);
        acc_x = new double[sz];
        acc_y = new double[sz];
        acc_z = new double[sz];
    }

    protected final void grow(int newsz) {
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

    public final void addAccels(int bodyno, double x, double y, double z) {
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

    protected final void optimize(BodyUpdates bb) {
        BodyUpdatesDouble b = (BodyUpdatesDouble) bb;
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

//    /**
//     * Object output serialization. Optimizes the object and then sends it
//     * out using the default write method.
//     * @param out the stream to write to.
//     */
//    private void writeObject(java.io.ObjectOutputStream out)
//            throws java.io.IOException {
//        optimizeAndTrim();
//        out.defaultWriteObject();
//    }

    /**
     * Adds the specified updates, preparing for an update round.
     * @param r the specified updates.
     */
    private void addUpdates(BodyUpdatesDouble r) {
        for (int i = 0; i < r.index; i++) {
            int ix = r.bodyNumbers[i];
            acc_x[ix] = r.acc_x[i];
            acc_y[ix] = r.acc_y[i];
            acc_z[ix] = r.acc_z[i];
        }
        if (r.more != null) {
            for (int i = 0; i < r.more.length; i++) {
                addUpdates((BodyUpdatesDouble) r.more[i]);
            }
            r.more = null;
        }
    }

    public final void prepareForUpdate() {
        int sz = computeSz();
        if (acc_x_static == null) {
            acc_x_static = new double[sz];
            acc_y_static = new double[sz];
            acc_z_static = new double[sz];
        } else if (sz != acc_x_static.length) {
            System.err.println("EEEK: something wrong with sizes!");
            System.exit(1);
        }
        for (int i = 0; i < index; i++) {
            int ix = bodyNumbers[i];
            acc_x_static[ix] = acc_x[i];
            acc_y_static[ix] = acc_y[i];
            acc_z_static[ix] = acc_z[i];
        }
        bodyNumbers = null;
        acc_x = acc_x_static;
        acc_y = acc_y_static;
        acc_z = acc_z_static;
        if (more != null) {
            for (int i = 0; i < more.length; i++) {
                addUpdates((BodyUpdatesDouble) more[i]);
            }
            more = null;
        }
    }

    public final void updateBodies(Body[] bodyArray, int iteration,
            RunParameters params) {
        for (int i = 0; i < bodyArray.length; i++) {
            bodyArray[i].computeNewPosition(iteration != 0,
                    acc_x[i], acc_y[i], acc_z[i],
                    params);
        }
    }

    /*
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        boolean b = in.readBoolean();
        if (b) {
            in.defaultReadObject();
        } else {
            int sz = in.readInt();
            if (acc_x_static == null) {
                acc_x_static = new double[sz];
                acc_y_static = new double[sz];
                acc_z_static = new double[sz];
            } else if (sz != acc_x_static.length) {
                System.err.println("EEEK: something wrong with sizes!");
                System.exit(1);
            }
            acc_x = acc_x_static;
            acc_y = acc_y_static;
            acc_z = acc_z_static;
            for (int i = 0; i < sz; i++) {
                acc_x[i] = in.readDouble();
                acc_y[i] = in.readDouble();
                acc_z[i] = in.readDouble();
            }
        }
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws java.io.IOException {
        if (bodyNumbers == null) {
            out.writeBoolean(false);
            out.writeInt(acc_x.length);
            for (int i = 0; i < acc_x.length; i++) {
                out.writeDouble(acc_x[i]);
                out.writeDouble(acc_y[i]);
                out.writeDouble(acc_z[i]);
            }
        } else {
            out.writeBoolean(true);
            out.defaultWriteObject();
        }
    }
    */
}
