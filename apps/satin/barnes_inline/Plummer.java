//import java.util.*;

/**
 * see ../../rmi/barnes/Plummer.java for the original version
 */

class Plummer {
    private RandomNumber rand;

    Plummer() {
	rand = new RandomNumber();
	rand.setSeed( 123 );
    }

    public Body[] generate(int numBodies) {

	int i, hNumBodies;
	double rsc, vsc, r, v, x, y, mass = 1.0/numBodies, offset = 4.0;
	double cmr_x = 0.0, cmr_y = 0.0, cmr_z = 0.0;
	double cmv_x = 0.0, cmv_y = 0.0, cmv_z = 0.0;
	double[] shell;

	Body[] bodies = new Body[numBodies];
	for (i = 0; i < numBodies; i++) {
	    bodies[i] = new Body();
	}

	rsc = 9.0 * Math.PI / 16.0;
	vsc = Math.sqrt(1.0 / rsc);

	hNumBodies = ( numBodies + 1 ) >> 1;

	for ( i=0; i<hNumBodies; i++ ) {

	    do {
		r = 1 / Math.sqrt(Math.pow( rand.xRand( 0.0, 0.999 ),
					     -2.0/3.0) - 1);
	    } while (r > 9.0);

	    shell = rand.pickShell( rsc * r );
	    bodies[i].pos_x = shell[0];
	    bodies[i].pos_y = shell[1];
	    bodies[i].pos_z = shell[2];
		
	    cmr_x += shell[0];
	    cmr_y += shell[1];
	    cmr_z += shell[2];

	    do {
		x = rand.xRand( 0.0, 1.0 );
		y = rand.xRand( 0.0, 0.1 );
	    } while (y > x*x * Math.pow(1 - x*x, 3.5));

	    v = Math.sqrt(2.0) * x / Math.pow(1 + r*r, 0.25);
  
	    shell = rand.pickShell( vsc * v );
	    
	    bodies[i].vel_x = shell[0];
	    bodies[i].vel_y = shell[1];
	    bodies[i].vel_z = shell[2];

	    cmv_x += shell[0];
	    cmv_y += shell[1];
	    cmv_z += shell[2];
	}


	for ( i=hNumBodies; i<numBodies; i++ ) {
	    bodies[i].pos_x = bodies[i-hNumBodies].pos_x + offset;

	    cmr_x += bodies[i].pos_x;
	    cmr_y += bodies[i].pos_y;
	    cmr_z += bodies[i].pos_z;

	    bodies[i].vel_x = bodies[i-hNumBodies].vel_x;

	    cmv_x += bodies[i].vel_x;
	    cmv_y += bodies[i].vel_y;
	    cmv_z += bodies[i].vel_z;

	    bodies[i].pos_y = bodies[i-hNumBodies].pos_y + offset;

	    cmr_x += bodies[i].pos_x;
	    cmr_y += bodies[i].pos_y;
	    cmr_z += bodies[i].pos_z;

	    bodies[i].vel_y = bodies[i-hNumBodies].vel_y;

	    cmv_x += bodies[i].vel_x;
	    cmv_y += bodies[i].vel_y;
	    cmv_z += bodies[i].vel_z;


	    bodies[i].pos_z = bodies[i-hNumBodies].pos_z + offset;

	    cmr_x += bodies[i].pos_x;
	    cmr_y += bodies[i].pos_y;
	    cmr_z += bodies[i].pos_z;

	    bodies[i].vel_z = bodies[i-hNumBodies].vel_z;

	    cmv_x += bodies[i].vel_x;
	    cmv_y += bodies[i].vel_y;
	    cmv_z += bodies[i].vel_z;
	}

	cmr_x /= (double)(numBodies);
	cmr_y /= (double)(numBodies);
	cmr_z /= (double)(numBodies);

	cmv_x /= (double)(numBodies);
	cmv_y /= (double)(numBodies);
	cmv_z /= (double)(numBodies);

	for ( i=0; i<numBodies; i++ ) {

	    bodies[i].number = i;

	    bodies[i].pos_x -= cmr_x;
	    bodies[i].pos_y -= cmr_y;
	    bodies[i].pos_z -= cmr_z;

	    bodies[i].vel_x -= cmv_x;
	    bodies[i].vel_y -= cmv_y;
	    bodies[i].vel_z -= cmv_z;

	    bodies[i].mass = mass;

	}

	return bodies;
    }

}
