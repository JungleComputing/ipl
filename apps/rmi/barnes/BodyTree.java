/* $Id$ */


strictfp
public class BodyTree extends BodyTreeNode {

    int side;
    vec3 min, max;

    // BodyTree is a special kind of BodyTreeNode; it is the root node of
    // the tree. It needs some extra routines for managing the tree

    BodyTree( GlobalData g, vec3 min, vec3 max ) {

	super( g );

	double hs, cx, cy, cz;

	// center the tree

	hs = (max.x - min.x)  * 0.5;

	cx = min.x + hs;
	cy = min.y + hs;
	cz = min.z + hs;

	Initialize( g, cx, cy, cz, hs, 0 );

	this.min = min;
	this.max = max;

	// Add all bodies here.

	for ( int i=0; i<g.gdNumBodies; i++ ) {
	    addBody( i );
	}

    }

    int ComputeAccelerationsBarnes() {

	return ComputeAccelerationsBarnes( this );
    }


    int ComputeAccelerationsDirect() {

	vec3   Diff;
	Body   b1, b2;
	double Dist, DistSq, Factor;
	int    interactions = 0, i, j;

	Diff = new vec3();

	for ( i=0; i<btnGd.gdNumBodies; i++ ) {

	    b1 = btnGd.gdBodies[i];

	    b1.bAcc.x = 0;
	    b1.bAcc.y = 0;
	    b1.bAcc.z = 0; 

	    for ( j=0; j<btnGd.gdNumBodies; j++ ) {

		b2 = btnGd.gdBodies[j];

		Diff.x = b2.bPos.x - b1.bPos.x;
		Diff.y = b2.bPos.y - b1.bPos.y;
		Diff.z = b2.bPos.z - b1.bPos.z;

		DistSq = Diff.x * Diff.x + Diff.y * Diff.y + Diff.z * Diff.z + btnGd.gdSoftSQ;
		Dist = Math.sqrt( DistSq );
		Factor = b2.bMass / (DistSq * Dist);

		b1.bAcc.x += Diff.x * Factor;
		b1.bAcc.y += Diff.y * Factor;
		b1.bAcc.z += Diff.z * Factor;

		interactions++;
	    }

	}
	return interactions;
    }



    void ComputeNewPositions() {

	int i;
	//    static vec3 dPos = null, dAcc, dVel, mVel;
	vec3 dPos = null, dAcc, dVel, mVel;
	Body b;

	//    if (dPos == null)
	{    
	    dPos = new vec3();
	    dAcc = new vec3();
	    dVel = new vec3();
	    mVel = new vec3();
	}


	for ( i=0; i<btnGd.gdNumBodies; i++ ) {

	    b = btnGd.gdBodies[i];
	    /*
	       btnGd.debugStr("updating pos for body " + b.bNumber + ": (" +
	       b.bAcc.x + ", " + b.bAcc.y + ", " + b.bAcc.z + "). ");
	       */
	    if ( btnGd.gdIteration > 0 ) {

		b.bVel.x += (b.bAcc.x - b.bOldAcc.x) * btnGd.gdDtHalf;
		b.bVel.y += (b.bAcc.y - b.bOldAcc.y) * btnGd.gdDtHalf;
		b.bVel.z += (b.bAcc.z - b.bOldAcc.z) * btnGd.gdDtHalf;
	    }

	    b.bPos.x += (b.bAcc.x * btnGd.gdDtHalf + b.bVel.x) * btnGd.gdDt;
	    b.bPos.y += (b.bAcc.y * btnGd.gdDtHalf + b.bVel.y) * btnGd.gdDt;
	    b.bPos.z += (b.bAcc.z * btnGd.gdDtHalf + b.bVel.z) * btnGd.gdDt;

	    b.bVel.x += b.bAcc.x * btnGd.gdDt; //vel += acc*dt
	    b.bVel.y += b.bAcc.y * btnGd.gdDt;
	    b.bVel.z += b.bAcc.z * btnGd.gdDt;

	    b.bOldAcc.x = b.bAcc.x;
	    b.bOldAcc.y = b.bAcc.y;
	    b.bOldAcc.z = b.bAcc.z;

	    /*
	       btnGd.debugStr("updating pos for body " + b.bNumber + ": (" +
	       b.bPos.x + ", " + b.bPos.y + ", " + b.bPos.z + "). ");
	       btnGd.debugStr("updating vel for body " + b.bNumber + ": (" +
	       b.bVel.x + ", " + b.bVel.y + ", " + b.bVel.z + "). ");
	       */
	}
    }

    //  void ComputeBounds() {

    void PoepChinees() {

	int i;
	double side;

	min.x = Double.MAX_VALUE;
	min.y = Double.MAX_VALUE;
	min.z = Double.MAX_VALUE;

	max.x = Double.MIN_VALUE;
	max.y = Double.MIN_VALUE;
	max.z = Double.MIN_VALUE;

	for ( i=0; i<btnGd.gdNumBodies; i++ ) {

	    min.x = Math.min( min.x,  btnGd.gdBodies[i].bPos.x ); 
	    min.y = Math.min( min.y,  btnGd.gdBodies[i].bPos.y ); 
	    min.z = Math.min( min.z,  btnGd.gdBodies[i].bPos.z ); 

	    max.x = Math.max( max.x,  btnGd.gdBodies[i].bPos.x ); 
	    max.y = Math.max( max.y,  btnGd.gdBodies[i].bPos.y ); 
	    max.z = Math.max( max.z,  btnGd.gdBodies[i].bPos.z ); 

	}

	// compute the cell size

	max.x = max.x - min.x;
	max.y = max.y - min.y;
	max.z = max.z - min.z;

	side = Math.max( Math.max( max.x, max.y ), max.z ); 

	// compute the new center and adjust the root node accordingly

	btnCenter_x = min.x + (side * (1.0 + btnGd.gdDimSlack ))/2; 
	btnCenter_y = min.y + (side * (1.0 + btnGd.gdDimSlack ))/2; 
	btnCenter_z = min.z + (side * (1.0 + btnGd.gdDimSlack ))/2;

	btnHalfSize = side; 
    }

    }


