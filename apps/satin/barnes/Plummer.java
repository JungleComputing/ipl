//import java.lang.*;
import java.util.*;

strictfp class Plummer {
	private Random rand;

	Plummer() {
		rand = new Random();
		rand.setSeed( 1230 );
	}

	private void pickShell( Vec3 point, double radius ) {

		double rsq, rsc;

		do {

			point.x = -1.0 + 2 * rand.nextDouble();
			point.y = -1.0 + 2 * rand.nextDouble();
			point.z = -1.0 + 2 * rand.nextDouble();

			// System.out.println("x = " + point.x + ", y = " + point.y + ", z = " + point.z);

			rsq = point.x * point.x + point.y * point.y + point.z * point.z;

		} while ( rsq > 1.0 );
    
		rsc = radius / Math.sqrt( rsq );

		point.x *= rsc;
		point.y *= rsc;
		point.z *= rsc;
	}

	public Body[] generate(int numBodies) {

		int i, hNumBodies;
		double rsc, vsc, r, v, x, y, mass = 1.0/numBodies, offset = 4.0;
		Vec3 cmr = new Vec3(), cmv = new Vec3();

		Body[] bodies = new Body[numBodies];
		for (i = 0; i < numBodies; i++) {
			bodies[i] = new Body();
			//bodies[i].weight = 100; we don't use weight - Maik
		}

		rsc = 9.0 * Math.PI / 16.0;
		vsc = Math.sqrt(1.0 / rsc);
		// System.out.println("rsc = " + rsc);
		// System.out.println("vsc = " + vsc);

		hNumBodies = ( numBodies + 1 ) >> 1;

		for ( i=0; i<hNumBodies; i++ ) {

			do {
				r = 1 / Math.sqrt( Math.pow(rand.nextDouble(), -2.0/3.0) - 1);
				// System.out.println("r = " + r);
			} while (r > 9.0);

			pickShell( bodies[i].pos, rsc * r );
			// System.out.println(" ");

			//System.out.println("i = " + i +
			//		   ", x = " + bodies[i].pos.x + 
			//		   ", y = " + bodies[i].pos.y + 
			//		   ", z = " + bodies[i].pos.z);
		
			cmr.x += bodies[i].pos.x;
			cmr.y += bodies[i].pos.y;
			cmr.z += bodies[i].pos.z;

			do {
				x = rand.nextDouble();
				y = rand.nextDouble() / 10.0;
			} while (y > x*x * Math.pow(1 - x*x, 3.5));

			v = Math.sqrt(2.0) * x / Math.pow(1 + r*r, 0.25);
  
			pickShell( bodies[i].vel, vsc * v );

			// System.out.println("i = " + i +
			//		   ", vx = " + bodies[i].bVel.x + 
			//		   ", vy = " + bodies[i].bVel.y + 
			//		   ", vz = " + bodies[i].bVel.z);

			cmv.x += bodies[i].vel.x;
			cmv.y += bodies[i].vel.y;
			cmv.z += bodies[i].vel.z;
		}


		for ( i=hNumBodies; i<numBodies; i++ ) {
			/*
			  // Is this a bug in the original Suel code? This one shows strange behaviour... 

			  bodies[i].pos.x = bodies[i-hNumBodies].pos.x + offset;      
			  bodies[i].pos.y = bodies[i-hNumBodies].pos.y + offset;      
			  bodies[i].pos.z = bodies[i-hNumBodies].pos.z + offset;      

			  cmr.x += bodies[i].pos.x;
			  cmr.y += bodies[i].pos.y;
			  cmr.z += bodies[i].pos.z;

			  bodies[i].bVel.x = bodies[i-hNumBodies].bVel.x;      
			  bodies[i].bVel.y = bodies[i-hNumBodies].bVel.y;      
			  bodies[i].bVel.z = bodies[i-hNumBodies].bVel.z;      

			  cmv.x += bodies[i].bVel.x;
			  cmv.y += bodies[i].bVel.y;
			  cmv.z += bodies[i].bVel.z;
			*/

			// This is compatible with the Suel version (using unrolled forloops).
			// It reads possibly uninitialized variables, so it may produce strange results... Weird!

			bodies[i].pos.x = bodies[i-hNumBodies].pos.x + offset;      

			cmr.x += bodies[i].pos.x;
			cmr.y += bodies[i].pos.y;
			cmr.z += bodies[i].pos.z;

			bodies[i].vel.x = bodies[i-hNumBodies].vel.x;

			cmv.x += bodies[i].vel.x;
			cmv.y += bodies[i].vel.y;
			cmv.z += bodies[i].vel.z;

			bodies[i].pos.y = bodies[i-hNumBodies].pos.y + offset;      

			cmr.x += bodies[i].pos.x;
			cmr.y += bodies[i].pos.y;
			cmr.z += bodies[i].pos.z;

			bodies[i].vel.y = bodies[i-hNumBodies].vel.y;      

			cmv.x += bodies[i].vel.x;
			cmv.y += bodies[i].vel.y;
			cmv.z += bodies[i].vel.z;

			bodies[i].pos.z = bodies[i-hNumBodies].pos.z + offset;      

			cmr.x += bodies[i].pos.x;
			cmr.y += bodies[i].pos.y;
			cmr.z += bodies[i].pos.z;

			bodies[i].vel.z = bodies[i-hNumBodies].vel.z;      

			cmv.x += bodies[i].vel.x;
			cmv.y += bodies[i].vel.y;
			cmv.z += bodies[i].vel.z;
		}


		cmr.x /= (double)(numBodies);
		cmr.y /= (double)(numBodies);
		cmr.z /= (double)(numBodies);

		cmv.x /= (double)(numBodies);
		cmv.y /= (double)(numBodies);
		cmv.z /= (double)(numBodies);

		for ( i=0; i<numBodies; i++ ) {

			bodies[i].number = i;

			bodies[i].pos.x -= cmr.x;
			bodies[i].pos.y -= cmr.y;
			bodies[i].pos.z -= cmr.z;

			bodies[i].vel.x -= cmv.x;
			bodies[i].vel.y -= cmv.y;
			bodies[i].vel.z -= cmv.z;

			bodies[i].mass = mass;

			bodies[i].acc.x = 0;
			bodies[i].acc.y = 0;
			bodies[i].acc.z = 0;

			bodies[i].oldAcc.x = 0;
			bodies[i].oldAcc.y = 0;
			bodies[i].oldAcc.z = 0;

			//	System.out.println("i = " + i + ", xpos = " + bodies[i].pos.x + ", ypos = " + bodies[i].pos.y + ", zpos = " + bodies[i].pos.z);
			//	System.out.println("i = " + i + ", xvel = " + bodies[i].vel.x + ", yvel = " + bodies[i].vel.y + ", zvel = " + bodies[i].vel.z);
		}

		return bodies;
	}

}
