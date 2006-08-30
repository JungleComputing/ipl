// File: $Id$

import java.util.Random;
import java.text.DecimalFormat;

public class Vehicle implements Configuration, java.io.Serializable {
    static Random r = new Random( 0 );
    static int labeler = 0;

    public Vehicle next;	/* Next entry in the linked list of its lane. */
    public int label;		/* The label of the vehicle. */
    private double position;	/* Position of the vehicle in meters from start. */
    private double velocity;	/* Speed of the vehicle in m/s. */
    private double preferedVelocity;

    protected Vehicle( double s, double ds )
    {
        preferedVelocity = s + ds*r.nextGaussian();
        velocity = preferedVelocity;
        label = labeler++;
    }

    public double getVelocity() { return velocity; }
    public double getPosition() { return position; }

    /** Does comfort adjustments to reach velocity v. */
    public boolean adjustToVelocity( double v )
    {
        if( v == velocity ){
            return false;
        }
        else if( v>velocity ){
            velocity = Math.min( velocity+COMFORTABLE_BRAKE_VELOCITY, v );
        }
        else {
            velocity = Math.max( velocity-COMFORTABLE_BRAKE_VELOCITY, v );
        }
        return true;
    }

    /** Does comfort adjustments to reach prefered velocity. */
    public boolean relaxVelocity()
    {
        return adjustToVelocity( preferedVelocity );
    }

    /** Does serious braking. */
    public void brake()
    {
        velocity -= STRONG_BRAKE_VELOCITY;
    }

    public boolean isDangerouslyClose( double pos )
    {
        return
            ((pos+SAFE_DISTANCE)>position) &&
            ((pos-SAFE_DISTANCE)<position);
    }

    public boolean isDangerouslyClose( Vehicle v )
    {
        return v != null && isDangerouslyClose( v.position );
    }

    public boolean isUncomfortablyClose( double pos )
    {
        return
            ((pos+COMFORTABLE_DISTANCE)>position) &&
            ((pos-COMFORTABLE_DISTANCE)<position);
    }

    public boolean isUncomfortablyClose( Vehicle v )
    {
        return v != null && isUncomfortablyClose( v.position );
    }

    public boolean isComfortableVelocity( double v )
    {
        return (v+SLOW_TOLERANCE)>=preferedVelocity;
    }

    public String toString()
    {
        return "@" + position + " v=" + velocity;
    }

    public void updatePosition()
    {
        position += velocity;
    }
}
