package ibis.ipl.apps.traffic;

// File: $Id$

class Car extends Vehicle implements Configuration {
    private static final long serialVersionUID = 1833979527967786L;

    public Car()
    {
        super( CAR_SPEED, CAR_SPEED_SD );
    }

    public String toString()
    {
        return "Car " + label + " " + super.toString();
    }
}
