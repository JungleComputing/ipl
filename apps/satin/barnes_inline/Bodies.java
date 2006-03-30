/* $Id$ */

public class Bodies implements BodiesInterface {

    Body[] bodyArray;
    
    BodyTreeNode bodyTreeRoot;

    RunParameters params;

    public Bodies(Body[] bodyArray, RunParameters params) {
        this.bodyArray = bodyArray;
	this.params = params;
	bodyTreeRoot = new BodyTreeNode(bodyArray, params);
        bodyTreeRoot.computeCentersOfMass();
    }
    
    
    public void updateBodies(double[] accs_x, double[] accs_y, double[] accs_z,
            int iteration) {

        for (int i = 0; i < bodyArray.length; i++) {
            bodyArray[i].computeNewPosition(iteration != 0, accs_x[i],
                    accs_y[i], accs_z[i], params);
        }  
	bodyTreeRoot = null; /*to prevent OutOfMemoryError (maik)*/
	bodyTreeRoot = new BodyTreeNode(bodyArray, params);
	bodyTreeRoot.computeCentersOfMass();
        // System.out.println("Body 0 updated after iteration " + iteration
        //         + ": " + bodyArray[0]);
    }    

    public void updateBodiesLocally(double[] accs_x, double[] accs_y,
            double[] accs_z, int iteration) {
        updateBodies(accs_x, accs_y, accs_z, iteration);
    }

    public BodyTreeNode getRoot() {
        return bodyTreeRoot;
    }
}
