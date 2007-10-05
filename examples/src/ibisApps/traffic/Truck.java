package ibisApps.traffic;

// File: $Id$

class Truck extends Vehicle implements Configuration {
    private static final long serialVersionUID = -727574653535668123L;

    public Truck()
    {
        super( TRUCK_SPEED, TRUCK_SPEED_SD );
    }

    public String toString()
    {
        return "Truck " + label + " " + super.toString();
    }
}
