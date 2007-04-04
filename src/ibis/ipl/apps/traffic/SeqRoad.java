package ibis.ipl.apps.traffic;

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
    static final boolean traceBraking = false;
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

            if( v != null && (lane<0 || v.getPosition()<pos) ){
                pos = v.getPosition();
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
            double frontpos = 2*ROAD_LENGTH;
            double frontVelocity = 2*ROAD_LENGTH;

            if( lane+1<LANES ){
                // If there is a car in the next lane, watch its speed.
                Vehicle vn = front[lane+1];
                if( vn != null ){
                    frontpos = vn.getPosition();
                    frontVelocity = vn.getVelocity();
                }
            }
            if( v.next != null ){
                // If there is a car in front of us, watch its speed.
                Vehicle vn = v.next;

                frontpos = Math.min( frontpos, vn.getPosition() );
                frontVelocity = Math.min( frontVelocity, vn.getVelocity() );
            }

            if( v.isDangerouslyClose( v.next ) ){
                // If we're too close to the next car in this lane, brake 
                v.brake();
                if( traceBraking ){
                    System.out.println( "T" + tick + ": brake for next vehicle: " + v  + " lane " + lane + " culpit: " + v.next );
                }
            }
            else if( v.isUncomfortablyClose( frontpos ) ){
                boolean switchedLane = false;
                if( !v.isComfortableVelocity( frontVelocity ) ){
                    // We want to switch to a faster lane.
                    if( lane+1<LANES ){
                        boolean safeSwitch = true;

                        if( v.isUncomfortablyClose( prev[lane+1] ) ){
                            // There is a vehicle in the next lane that blocks
                            // a lane switch.
                            safeSwitch = false;
                        }
                        if( v.isUncomfortablyClose( front[lane+1] )){
                            // There is a vehicle in the next lane that blocks
                            // a lane switch.
                            safeSwitch = false;
                        }

                        if( safeSwitch ){
                            // Ok, switch lanes.
                            if( traceLaneSwitching ){
                                System.out.println( "T" + tick + ": switch lane " + lane + "->" + (lane+1) + ": " + v  + " culpit: " + v.next );
                            }
                            // Extract the vehicle from this lane.
                            if( prev[lane] == null ){
                                lanes[lane] = v.next;
                            }
                            else {
                                prev[lane].next = v.next;
                            }
                            front[lane] = v.next;

                            lane++;

                            // Insert the vehicle in the next lane.
                            v.next = front[lane];
                            if( prev[lane] == null ){
                                lanes[lane] = v;
                            }
                            else {
                                prev[lane].next = v;
                            }
                            front[lane] = v;
                            switchedLane = true;
                        }
                    }
                }
                if( !switchedLane ){
                    // Too close for comfort but safe, adapt velocity.
                    boolean changed = v.adjustToVelocity( frontVelocity );
                    if( changed && traceBraking ){
                        System.out.println( "T" + tick + ": comfort adjustment of " + v  + " lane " + lane );
                    }
                }
            }
            else {
                // We're not close to a car, adjust velocity to what we like.
                boolean changed = v.relaxVelocity();
                if( changed && traceBraking ){
                    System.out.println( "T" + tick + ": relaxed velocity of " + v  + " lane " + lane );
                }
                // We want to switch to a faster lane.
                if( lane>0 ){
                    boolean safeSwitch = true;

                    if( v.isUncomfortablyClose( prev[lane-1] ) ){
                        // There is a vehicle in the previous lane that blocks
                        // a lane switch.
                        safeSwitch = false;
                    }
                    if( v.isUncomfortablyClose( front[lane-1] )){
                        // There is a vehicle in the previous lane that blocks
                        // a lane switch.
                        safeSwitch = false;
                    }

                    if( safeSwitch ){
                        // Ok, switch lanes.
                        if( traceLaneSwitching ){
                            System.out.println( "T" + tick + ": switch lane " + lane + "->" + (lane-1) + ": " + v  );
                        }
                        // Extract the vehicle from this lane.
                        if( prev[lane] == null ){
                            lanes[lane] = v.next;
                        }
                        else {
                            prev[lane].next = v.next;
                        }
                        front[lane] = v.next;

                        lane--;

                        // Insert the vehicle in the next lane.
                        v.next = front[lane];
                        if( prev[lane] == null ){
                            lanes[lane] = v;
                        }
                        else {
                            prev[lane].next = v;
                        }
                        front[lane] = v;
                    }
                }
            }
            v.updatePosition();
            if( v.getPosition() >= ROAD_LENGTH ){
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
