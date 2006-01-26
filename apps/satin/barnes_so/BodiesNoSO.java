/* $Id$ */

public class BodiesNoSO implements java.io.Serializable {

    Body[] bodyArray;
    
    transient BodyTreeNode bodyTreeRoot;

    int numBodies;

    int maxLeafBodies;

    double theta;
    
    public BodiesNoSO(int numBodies, int maxLeafBodies, double theta) {
    
        this.numBodies = numBodies;
	this.maxLeafBodies = maxLeafBodies;
	this.theta = theta;
        bodyArray = new Plummer().generate(numBodies);
	bodyTreeRoot = new BodyTreeNode(bodyArray, maxLeafBodies, theta);
        bodyTreeRoot.computeCentersOfMass();
    }
    
    
    public void updateBodies(double[] accs_x, double[] accs_y, double[] accs_z, int iteration) {

        for (int i = 0; i < bodyArray.length; i++) {
            bodyArray[i].computeNewPosition(iteration != 0, BarnesHut.DT, accs_x[i],
                    accs_y[i], accs_z[i]);
        }  
	bodyTreeRoot = null; /*to prevent OutOfMemoryError (maik)*/
	bodyTreeRoot = new BodyTreeNode(bodyArray, maxLeafBodies, theta);
	bodyTreeRoot.computeCentersOfMass();
    }    


}
