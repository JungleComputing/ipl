/* $Id$ */

import ibis.satin.WriteMethodsInterface;

public interface BodiesSOInterface extends WriteMethodsInterface {
    public void updateBodies(double[] accs_x, double[] accs_y, double[] accs_z,
            int iteration);
}
