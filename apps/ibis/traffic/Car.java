// File: $Id$

class Car extends Vehicle implements Configuration {
    public Car()
    {
	super( CAR_SPEED, CAR_SPEED_SD );
    }

    public String toString()
    {
	return "Car " + label + " " + super.toString();
    }
}
