/* $Id: */

import ibis.satin.SharedObject;

final public class BodiesSO extends SharedObject implements BodiesInterface, BodiesSOInterface, java.io.Serializable {

    Body[] bodyArray;

    transient BodyTreeNode bodyTreeRoot;

    RunParameters params;

    int iteration = -1;

    public BodiesSO(Body[] bodyArray, RunParameters params) {
        this.bodyArray = bodyArray;
        this.params = params;
        bodyTreeRoot = new BodyTreeNode(bodyArray, params);
        bodyTreeRoot.computeCentersOfMass();
    }

    /*write method*/
    public void updateBodies(double[] accs_x, double[] accs_y, double[] accs_z,
            int iteration) {
        // Oops: when a node joins while node 0 broadcasts the update,
        // and the node obtains an already updated object while it has
        // not received the update yet, when it receives the update, it will
        // update again, which is wrong.
        if (iteration == this.iteration+1) {
            updateBodiesLocally(accs_x, accs_y, accs_z, iteration);
        }
    }

    public void updateBodiesLocally(double[] accs_x, double[] accs_y,
            double[] accs_z, int iteration) {

        for (int i = 0; i < bodyArray.length; i++) {
            bodyArray[i].computeNewPosition(iteration != 0,
                    accs_x[i], accs_y[i], accs_z[i], params);
        }  

	bodyTreeRoot = null; /*to prevent OutOfMemoryError (maik)*/
	bodyTreeRoot = new BodyTreeNode(bodyArray, params);
	bodyTreeRoot.computeCentersOfMass();
        this.iteration = iteration;
        // System.out.println("Body 0 updated after iteration " + iteration
        //         + ": " + bodyArray[0]);
    }    

    public BodyTreeNode findTreeNode(byte[] treeNodeIdentifier) {

        /*no consistency check for the time being*/
        if (bodyTreeRoot == null) {
            System.err.println("bodyTreeRoot is null!");
        }

        BodyTreeNode treeNode = bodyTreeRoot;
        if (treeNodeIdentifier != null) {
            for (int i = 0; i < treeNodeIdentifier.length; i++) {
                treeNode = treeNode.children[treeNodeIdentifier[i]];
            }
        }
        return treeNode;
    }

    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        
        in.defaultReadObject();

        // Also read velocities and old accellerations when needed.
        // They are transient in Body.java, because the NTC version does
        // not need them on the other nodes. The SO version does, however,
        // because it updates by shipping accellerations.
        for (int i = 0; i < bodyArray.length; i++) {
            bodyArray[i].vel_x = in.readDouble();
            bodyArray[i].vel_y = in.readDouble();
            bodyArray[i].vel_z = in.readDouble();
        }
        // System.out.println("Read body 0 after iteration " + iteration
        //         + ": " + bodyArray[0]);
        if (iteration >= 0) {
            // System.out.println("Reading accellerations");
            for (int i = 0; i < bodyArray.length; i++) {
                bodyArray[i].oldAcc_x = in.readDouble();
                bodyArray[i].oldAcc_y = in.readDouble();
                bodyArray[i].oldAcc_z = in.readDouble();
            }
        }
        
        bodyTreeRoot = new BodyTreeNode(bodyArray, params);
        bodyTreeRoot.computeCentersOfMass();
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws java.io.IOException {
        
        out.defaultWriteObject();
        // System.out.println("Wrote body 0 after iteration " + iteration
        //         + ": " + bodyArray[0]);
        // Also write velocities and old accellerations when needed.
        // They are transient in Body.java, because the NTC version does
        // not need them on the other nodes. The SO version does, however,
        // because it updates by shipping accellerations.
        for (int i = 0; i < bodyArray.length; i++) {
            out.writeDouble(bodyArray[i].vel_x);
            out.writeDouble(bodyArray[i].vel_y);
            out.writeDouble(bodyArray[i].vel_z);
        }
        if (iteration >= 0) {
            // System.out.println("Writing accellerations");
            for (int i = 0; i < bodyArray.length; i++) {
                out.writeDouble(bodyArray[i].oldAcc_x);
                out.writeDouble(bodyArray[i].oldAcc_y);
                out.writeDouble(bodyArray[i].oldAcc_z);
            }
        }
    }

    public BodyTreeNode getRoot() {
        return bodyTreeRoot;
    }
}
