// File: $Id$

class Truck extends Vehicle implements Configuration {
    public Truck() {
        super(TRUCK_SPEED, TRUCK_SPEED_SD);
    }

    public String toString() {
        return "Truck " + label + " " + super.toString();
    }
}