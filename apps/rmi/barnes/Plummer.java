/* $Id$ */

strictfp
class Plummer {

    private RandomNumber Rand;

    Plummer( RandomNumber r ) {
	Rand = r;
    }

    void Generate( Body bodies[], int numBodies ) {

	int i, hNumBodies;
	double rsc, vsc, r, v, x, y, mass = 1.0/numBodies, offset = 4.0;
	vec3 cmr = new vec3(), cmv = new vec3();

	Rand.setSeed( 123 );

	rsc = 9.0 * Math.PI / 16.0;
	vsc = Math.sqrt(1.0 / rsc);
	// System.out.println("rsc = " + rsc);
	// System.out.println("vsc = " + vsc);

	hNumBodies = ( numBodies + 1 ) >> 1;

	for ( i=0; i<hNumBodies; i++ ) {

	    do {
		r = 1 / Math.sqrt( Math.pow( Rand.xRand( 0.0, 0.999 ), -2.0/3.0) - 1);
		// System.out.println("r = " + r);
	    } while (r > 9.0);

	    Rand.pickShell( bodies[i].bPos, rsc * r );
	    // System.out.println(" ");

	    //System.out.println("i = " + i +
	    //		   ", x = " + bodies[i].bPos.x + 
	    //		   ", y = " + bodies[i].bPos.y + 
	    //		   ", z = " + bodies[i].bPos.z);

	    cmr.x += bodies[i].bPos.x;
	    cmr.y += bodies[i].bPos.y;
	    cmr.z += bodies[i].bPos.z;

	    do {
		x = Rand.xRand( 0.0, 1.0 );
		y = Rand.xRand( 0.0, 0.1 );
	    } while (y > x*x * Math.pow(1 - x*x, 3.5));

	    v = Math.sqrt(2.0) * x / Math.pow(1 + r*r, 0.25);

	    Rand.pickShell( bodies[i].bVel, vsc * v );

	    // System.out.println("i = " + i +
	    //		   ", vx = " + bodies[i].bVel.x + 
	    //		   ", vy = " + bodies[i].bVel.y + 
	    //		   ", vz = " + bodies[i].bVel.z);

	    cmv.x += bodies[i].bVel.x;
	    cmv.y += bodies[i].bVel.y;
	    cmv.z += bodies[i].bVel.z;
	}


	for ( i=hNumBodies; i<numBodies; i++ ) {
	    /*
	    // Is this a bug in the original Suel code? This one shows strange behaviour... 

	    bodies[i].bPos.x = bodies[i-hNumBodies].bPos.x + offset;      
	    bodies[i].bPos.y = bodies[i-hNumBodies].bPos.y + offset;      
	    bodies[i].bPos.z = bodies[i-hNumBodies].bPos.z + offset;      

	    cmr.x += bodies[i].bPos.x;
	    cmr.y += bodies[i].bPos.y;
	    cmr.z += bodies[i].bPos.z;

	    bodies[i].bVel.x = bodies[i-hNumBodies].bVel.x;      
	    bodies[i].bVel.y = bodies[i-hNumBodies].bVel.y;      
	    bodies[i].bVel.z = bodies[i-hNumBodies].bVel.z;      

	    cmv.x += bodies[i].bVel.x;
	    cmv.y += bodies[i].bVel.y;
	    cmv.z += bodies[i].bVel.z;
	    */

	    // This is compatible with the Suel version (using unrolled forloops).
	    // It reads possibly uninitialized variables, so it may produce strange results... Weird!

	    bodies[i].bPos.x = bodies[i-hNumBodies].bPos.x + offset;      

	    cmr.x += bodies[i].bPos.x;
	    cmr.y += bodies[i].bPos.y;
	    cmr.z += bodies[i].bPos.z;

	    bodies[i].bVel.x = bodies[i-hNumBodies].bVel.x;      

	    cmv.x += bodies[i].bVel.x;
	    cmv.y += bodies[i].bVel.y;
	    cmv.z += bodies[i].bVel.z;

	    bodies[i].bPos.y = bodies[i-hNumBodies].bPos.y + offset;      

	    cmr.x += bodies[i].bPos.x;
	    cmr.y += bodies[i].bPos.y;
	    cmr.z += bodies[i].bPos.z;

	    bodies[i].bVel.y = bodies[i-hNumBodies].bVel.y;      

	    cmv.x += bodies[i].bVel.x;
	    cmv.y += bodies[i].bVel.y;
	    cmv.z += bodies[i].bVel.z;

	    bodies[i].bPos.z = bodies[i-hNumBodies].bPos.z + offset;      

	    cmr.x += bodies[i].bPos.x;
	    cmr.y += bodies[i].bPos.y;
	    cmr.z += bodies[i].bPos.z;

	    bodies[i].bVel.z = bodies[i-hNumBodies].bVel.z;      

	    cmv.x += bodies[i].bVel.x;
	    cmv.y += bodies[i].bVel.y;
	    cmv.z += bodies[i].bVel.z;
	}


	cmr.x /= (double)(numBodies);
	cmr.y /= (double)(numBodies);
	cmr.z /= (double)(numBodies);

	cmv.x /= (double)(numBodies);
	cmv.y /= (double)(numBodies);
	cmv.z /= (double)(numBodies);

	for ( i=0; i<numBodies; i++ ) {

	    bodies[i].bNumber = i;

	    bodies[i].bPos.x -= cmr.x;
	    bodies[i].bPos.y -= cmr.y;
	    bodies[i].bPos.z -= cmr.z;

	    bodies[i].bVel.x -= cmv.x;
	    bodies[i].bVel.y -= cmv.y;
	    bodies[i].bVel.z -= cmv.z;

	    bodies[i].bMass = mass;

	    bodies[i].bAcc.x = 0;
	    bodies[i].bAcc.y = 0;
	    bodies[i].bAcc.z = 0;

	    bodies[i].bOldAcc.x = 0;
	    bodies[i].bOldAcc.y = 0;
	    bodies[i].bOldAcc.z = 0;

	    //	System.out.println("i = " + i + ", xpos = " + bodies[i].bPos.x + ", ypos = " + bodies[i].bPos.y + ", zpos = " + bodies[i].bPos.z);
	    //	System.out.println("i = " + i + ", xvel = " + bodies[i].bVel.x + ", yvel = " + bodies[i].bVel.y + ", zvel = " + bodies[i].bVel.z);
	}
    }

}
