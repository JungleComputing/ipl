/* $Id$ */

/*
 * Active tuple: does the sequential work in the tuple2 version.
 * A tuple contains the computed accelerations in the last force calculation
 * phase
 */

final class TreeUpdater implements ibis.satin.ActiveTuple {
    double[] accs_x;

    double[] accs_y;

    double[] accs_z;

    TreeUpdater(double[] x, double[] y, double[] z) {
        accs_x = x;
        accs_y = y;
        accs_z = z;
    }

    public void handleTuple(String key) {
        int iteration = Integer.parseInt(key);

        if (iteration > 0) {
            //the updates from the previous iteration have to be applied
            BarnesHut.updateBodies(accs_x, accs_y, accs_z, iteration - 1);
        }

        BarnesHut.buildTreeAndDoCoM();
    }
}