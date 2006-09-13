/* $Id$ */

public final class Bodies implements BodiesInterface {

    Body[] bodyArray;
    
    BodyTreeNode bodyTreeRoot;

    RunParameters params;

    public Bodies(Body[] bodyArray, RunParameters params) {
        this.bodyArray = bodyArray;
	this.params = params;
	bodyTreeRoot = new BodyTreeNode(bodyArray, params);
        bodyTreeRoot.computeCentersOfMass();
    }
    
    
    public void updateBodies(BodyUpdates b, int iteration) {
        updateBodiesLocally(b, iteration);
    }    

    public void updateBodiesLocally(BodyUpdates b, int iteration) {
        b.updateBodies(bodyArray, iteration, params);
	bodyTreeRoot = null; /*to prevent OutOfMemoryError (maik)*/
	bodyTreeRoot = new BodyTreeNode(bodyArray, params);
	bodyTreeRoot.computeCentersOfMass();
        // System.out.println("Body 0 updated after iteration " + iteration
        //         + ": " + bodyArray[0]);
    }

    public BodyTreeNode getRoot() {
        return bodyTreeRoot;
    }
}
