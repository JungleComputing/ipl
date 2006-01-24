import ibis.satin.SharedObject;

final public class Bodies extends SharedObject implements BodiesInterface {

    Body[] bodyArray;

    transient BodyTreeNode bodyTreeRoot;

    int numBodies;

    int maxLeafBodies;

    double theta;

    public int iteration = -1;

    public Bodies(int numBodies, int maxLeafBodies, double theta) {

        this.numBodies = numBodies;
        this.maxLeafBodies = maxLeafBodies;
        this.theta = theta;
        bodyArray = new Plummer().generate(numBodies);
        bodyTreeRoot = new BodyTreeNode(bodyArray, maxLeafBodies, theta);
        bodyTreeRoot.computeCentersOfMass();
    }

    /*write method*/
    public void updateBodies(double[] accs_x, double[] accs_y, double[] accs_z,
            int iteration) {
        updateBodiesLocally(accs_x, accs_y, accs_z, iteration);
    }

    public void updateBodiesLocally(double[] accs_x, double[] accs_y,
            double[] accs_z, int iteration) {

        for (int i = 0; i < bodyArray.length; i++) {
            bodyArray[i].computeNewPosition(iteration != 0, BarnesHut.DT,
                accs_x[i], accs_y[i], accs_z[i]);
        }
        bodyTreeRoot = null; /*to prevent OutOfMemoryError (maik)*/
        bodyTreeRoot = new BodyTreeNode(bodyArray, maxLeafBodies, theta);
        bodyTreeRoot.computeCentersOfMass();
        this.iteration = iteration;
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
//        long start = System.currentTimeMillis();
        
        in.defaultReadObject();
        
//        double time = System.currentTimeMillis() - start;
//        long rebuildStart = System.currentTimeMillis(); 

        bodyTreeRoot = new BodyTreeNode(bodyArray, maxLeafBodies, theta);
        bodyTreeRoot.computeCentersOfMass();
        
//        double rebuildTime = System.currentTimeMillis() - rebuildStart;
//        System.err.println("readTree: deserialization = " + time + " ms, rebuild = " + rebuildTime + " ms");
    }
}
