/* $Id$ */

//import java.util.*;

/**
 * see ../../rmi/barnes/Plummer.java for the original version
 */

class Plummer {
    private RandomNumber rand;

    Plummer() {
        rand = new RandomNumber();
        rand.setSeed(123);
    }

    public Body[] generate(int numBodies) {

        int i, hNumBodies;
        double rsc, vsc, r, v, x, y, mass = 1.0 / numBodies, offset = 4.0;
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

        hNumBodies = (numBodies + 1) >> 1;

        for (i = 0; i < hNumBodies; i++) {

            do {
                r = 1 / Math
                        .sqrt(Math.pow(rand.xRand(0.0, 0.999), -2.0 / 3.0) - 1);
                //System.out.println("r = " + r);
            } while (r > 9.0);

            rand.pickShell(bodies[i].pos, rsc * r);
            // System.out.println(" ");

            //System.out.println("i = " + i +
            //		   ", x = " + bodies[i].pos.x + 
            //		   ", y = " + bodies[i].pos.y + 
            //		   ", z = " + bodies[i].pos.z);

            cmr.add(bodies[i].pos);

            do {
                x = rand.xRand(0.0, 1.0);
                y = rand.xRand(0.0, 0.1);
            } while (y > x * x * Math.pow(1 - x * x, 3.5));

            v = Math.sqrt(2.0) * x / Math.pow(1 + r * r, 0.25);

            rand.pickShell(bodies[i].vel, vsc * v);

            // System.out.println("i = " + i +
            //		   ", vx = " + bodies[i].bVel.x + 
            //		   ", vy = " + bodies[i].bVel.y + 
            //		   ", vz = " + bodies[i].bVel.z);

            cmv.add(bodies[i].vel);
        }

        for (i = hNumBodies; i < numBodies; i++) {
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
            bodies[i].pos.x = bodies[i - hNumBodies].pos.x + offset;

            cmr.add(bodies[i].pos);

            bodies[i].vel.x = bodies[i - hNumBodies].vel.x;

            cmv.add(bodies[i].vel);

            bodies[i].pos.y = bodies[i - hNumBodies].pos.y + offset;

            cmr.add(bodies[i].pos);

            bodies[i].vel.y = bodies[i - hNumBodies].vel.y;

            cmv.add(bodies[i].vel);

            bodies[i].pos.z = bodies[i - hNumBodies].pos.z + offset;

            cmr.add(bodies[i].pos);

            bodies[i].vel.z = bodies[i - hNumBodies].vel.z;

            cmv.add(bodies[i].vel);
        }

        cmr.div((double) (numBodies));
        cmv.div((double) (numBodies));

        for (i = 0; i < numBodies; i++) {

            bodies[i].number = i;

            bodies[i].pos.sub(cmr);

            bodies[i].vel.sub(cmv);

            bodies[i].mass = mass;

            //	System.out.println("i = " + i + ", xpos = " + bodies[i].pos.x + ", ypos = " + bodies[i].pos.y + ", zpos = " + bodies[i].pos.z);
            //	System.out.println("i = " + i + ", xvel = " + bodies[i].vel.x + ", yvel = " + bodies[i].vel.y + ", zvel = " + bodies[i].vel.z);
        }

        return bodies;
    }

}