// File: $Id$

import java.util.Random;

public class Vehicle implements Configuration, java.io.Serializable {
    static Random r = new Random( 0 );
    static int labeler = 0;

    Vehicle next;	/* Next entry in the linked list of its lane. */
    int label;		/* The label of the vehicle. */
    double position;	/* Position of the vehicle in meters from start. */
    double velocity;	/* Speed of the vehicle in m/s. */
    double preferedVelocity;

    protected Vehicle( double s, double ds )
    {
	preferedVelocity = s + ds*r.nextGaussian();
	label = labeler++;
    }

    void updatePosition( Vehicle front )
    {
	if( front != null && position+SAFE_DISTANCE<front.position ){
	    velocity = front.velocity;
	}
	else {
	    velocity = preferedVelocity;
	}
	position += velocity;
    }
}
