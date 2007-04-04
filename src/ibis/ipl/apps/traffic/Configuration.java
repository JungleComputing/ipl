// File: $Id$

interface Configuration {
    static final double MS2KMH = 3.6;
    static final double KMH2MS = 1.0/MS2KMH;
    static final double TRUCK_SPEED = 80.0 * KMH2MS;
    static final double TRUCK_SPEED_SD = 2.0 * KMH2MS;
    static final double CAR_SPEED = 100.0 * KMH2MS;
    static final double CAR_SPEED_SD = 5.0 * KMH2MS;
    static final double COMFORTABLE_DISTANCE = 4.0*CAR_SPEED;
    static final double SAFE_DISTANCE = 2.0*CAR_SPEED;
    static final double ROAD_LENGTH = 50e3;

    static final double COMFORTABLE_BRAKE_VELOCITY = 5.0 * KMH2MS;
    static final double STRONG_BRAKE_VELOCITY = 10.0 * KMH2MS;
    static final double SLOW_TOLERANCE = 3.0 * KMH2MS;

    static final int LANES = 3;
}
