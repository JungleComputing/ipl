/**
 * Active tuple: initializes the BarnesHut app at each node
 */

final class Initializer implements ibis.satin.ActiveTuple {
    int nBodies;       //number of bodies in the tree
    int maxLeafBodies; //max. bodies per leaf node

    Initializer(int n, int m) {
	nBodies = n;
	maxLeafBodies = m;
    }

    public void handleTuple(String key) {
	BarnesHut.initialize(nBodies, maxLeafBodies);
    }
}
