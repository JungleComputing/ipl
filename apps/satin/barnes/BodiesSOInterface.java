/* $Id$ */

import ibis.satin.WriteMethodsInterface;

public interface BodiesSOInterface extends WriteMethodsInterface {
    public void updateBodies(BodyUpdates b, int iteration);
}
