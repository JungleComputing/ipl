// File: $Id$

import java.util.Random;

public class Vehicle implements java.io.Serializable {
    Vehicle next;	/* Next entry in the linked list of its lane. */
    double position;	/* Position of the vehicle in meters from start. */
    double velocity;	/* Speed of the vehicle in m/s. */
    double preferedVelocity;
    Random r = new Random( 0 );

    protected Vehicle( double s, double ds )
    {
	preferedVelocity = s + ds*r.nextGaussian();
    }

    void updatePosition()
    {
	position += velocity;
    }
}
