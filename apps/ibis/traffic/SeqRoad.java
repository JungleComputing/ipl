// File: $Id$

import java.util.Random;

class SeqRoad implements Configuration {
    final int TICKS = 100000;
    final double launchInterval = 3.1;
    double launchElapsed = launchInterval;
    static final int CARS_PER_TRUCK = 3;
    Vehicle lanes[] = new Vehicle[LANES];
    Random r = new Random( 0 );
    static final boolean traceVehicleUpdates = true;

    SeqRoad()
    {
    }

    private void launchVehicle()
    {
	Vehicle v;

	// TODO: make sure we're not too close to the previous one.
	if( r.nextInt( CARS_PER_TRUCK ) == 0 ){
	    v = new Truck();
	}
	else {
	    v = new Car();
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
    private void updateVehiclePositions()
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

	    v.updatePosition( v.next );
	    front[lane] = v.next;
	}

	// TODO: retire vehicles.
    }

    private void runTick()
    {
	updateVehiclePositions();
	if( launchElapsed >= launchInterval ){
	    launchElapsed -= launchInterval;
	    launchVehicle();
	}
    }

    private void runSimulation()
    {
	for( int tick=0; tick<TICKS; tick++ ){
	    runTick();
	}
    }

    public static void main( String args[] )
    {
	SeqRoad r = new SeqRoad();
	
	r.runSimulation();
    }
}
