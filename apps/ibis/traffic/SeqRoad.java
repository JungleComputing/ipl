// File: $Id$

import java.util.Random;

class SeqRoad implements Configuration {
    final int TICKS = 100000;
    final double launchInterval = 5.1;
    double launchElapsed = launchInterval;
    static final int CARS_PER_TRUCK = 4;
    Vehicle lanes[] = new Vehicle[LANES];
    Random r = new Random( 0 );
    static final boolean traceVehicleUpdates = false;
    static final boolean traceCreateRetire = true;
    static final boolean traceBraking = true;
    static final boolean traceLaneSwitching = true;

    SeqRoad()
    {
    }

    private void launchVehicle( int tick )
    {
	Vehicle v;

	// TODO: make sure we're not too close to the previous one.
	if( r.nextInt( CARS_PER_TRUCK ) == 0 ){
	    v = new Truck();
	}
	else {
	    v = new Car();
	}
	if( traceCreateRetire ){
	    System.out.println( "T" + tick + ": launching " + v );
	}
	v.next = lanes[0];
	lanes[0] = v;
    }

    /** Given the position of the current vehicle in each lane,
     * select the lane with the backmost one.
     */
    private static int selectLane( Vehicle positions[] )
    {
	int lane = -1;
	double pos = 0.0;

	for( int i=0; i<positions.length; i++ ){
	    Vehicle v = positions[i];

	    if( v != null && (lane<0 || v.position<pos) ){
		pos = v.position;
		lane = i;
	    }
	}
	return lane;
    }

    /** Updates all car positions in a back-to-front wavefront.
     * Cars slow down to avoid hitting the one in front, and switch
     * lanes if their speed is too low.
     */
    private void updateVehiclePositions( int tick )
    {
	/** For each lane, the first car we haven't updated yet. */
	Vehicle front[] = (Vehicle []) lanes.clone();

	/** For each lane, the position of the car behind the wave front. */
	Vehicle prev[] = new Vehicle[LANES];

	while( true ) {
	    int lane = selectLane( front );

	    if( lane<0 ){
		break;
	    }
	    Vehicle v = front[lane];

	    if( traceVehicleUpdates ){
		System.out.println( "Update vehicle in lane " + lane + ": " + v );
	    }
	    Vehicle vfront = v.next;
	    double frontpos = v.position+2*SAFE_DISTANCE;
	    double frontVelocity = 2*v.preferedVelocity;

	    if( lane+1<LANES ){
		// If there is a car in the next lane, watch its speed.
		Vehicle vn = front[lane+1];
		if( vn != null ){
		    frontpos = vn.position;
		    frontVelocity = vn.velocity;
		}
	    }
	    if( v.next != null ){
		// If there is a car in front of us, watch its speed.
		Vehicle vn = v.next;

		frontpos = Math.min( frontpos, vn.position );
		frontVelocity = Math.min( frontVelocity, vn.velocity );
	    }

	    if( v.position+SAFE_DISTANCE>frontpos ){
		// If we're too close, brake 
		v.velocity = v.velocity-STRONG_BRAKE_VELOCITY;
		if( traceBraking ){
		    System.out.println( "T" + tick + ": strong braking of " + v );
		}
	    }
	    else if( v.position+COMFORTABLE_DISTANCE>frontpos ){
		// Too close for comfort but safe, adapt velocity.
		v.velocity = Math.max( v.velocity-COMFORTABLE_BRAKE_VELOCITY, frontVelocity );
		if( v.velocity<frontVelocity ){
		    v.velocity = Math.min( frontVelocity, v.velocity+COMFORTABLE_BRAKE_VELOCITY );
		}
		if( traceBraking ){
		    System.out.println( "T" + tick + ": comfort adjustment of " + v );
		}
	    }
	    else {
		v.velocity = v.preferedVelocity;
	    }
	    v.position += v.velocity;
	    if( v.position>= ROAD_LENGTH ){
		// Retire this car.
		if( traceCreateRetire ){
		    System.out.println( "T" + tick + ": retiring " + v );
		}
		if( prev[lane] == null ){
		    lanes[lane] = v.next;
		}
		else {
		    prev[lane].next = v.next;
		}
	    }
	    else {
		prev[lane] = front[lane];
	    }
	    front[lane] = v.next;
	}

	// TODO: retire vehicles.
    }

    private void runTick( int tick )
    {
	updateVehiclePositions( tick );
	if( launchElapsed >= launchInterval ){
	    launchElapsed -= launchInterval;
	    launchVehicle( tick );
	}
	launchElapsed += 1.0;
    }

    private void runSimulation()
    {
	for( int tick=0; tick<TICKS; tick++ ){
	    runTick( tick );
	}
    }

    public static void main( String args[] )
    {
	SeqRoad r = new SeqRoad();
	
	r.runSimulation();
    }
}
