/* $Id$ */

public interface BodiesInterface {
    public void updateBodies(double[] accs_x, double[] accs_y, double[] accs_z,
            int iteration);

    // To be called after the last iteration, instead of updateBodies.
    public void updateBodiesLocally(double[] accs_x, double[] accs_y,
                        double[] accs_z, int iteration);

    public BodyTreeNode getRoot();
}
