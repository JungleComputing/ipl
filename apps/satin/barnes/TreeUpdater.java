/*
 * Active tuple: does the sequential work in the tuple2 version.
 * A tuple contains the computed accelerations in the last force calculation
 * phase
 */

final class TreeUpdater implements ibis.satin.ActiveTuple {
    Vec3[] accs;

    TreeUpdater(Vec3[] a) {
        accs = a;
    }

    public void handleTuple(String key) {
        System.out.println("Updating at iteration " + key);
        int iteration = Integer.parseInt(key);

        if (iteration > 0) {
            //the updates from the previous iteration have to be applied
            BarnesHut.updateBodies(accs, iteration - 1);
        }

        BarnesHut.buildTreeAndDoCoM();
    }
}