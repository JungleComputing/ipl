/* $Id$ */

import java.io.*;

public final class Body implements Cloneable, Comparable, Serializable {

	public int number;

	public double pos_x, pos_y, pos_z;

	public double mass;

	// these are only used by calculateNewPosition, which is done at the
	// main node, so they can be transient
/*	transient*/ public double vel_x, vel_y, vel_z;

/*	transient*/ public double oldAcc_x, oldAcc_y, oldAcc_z;

	// these are used by the stable version to store the calculated acc
	transient public double acc_x, acc_y, acc_z;

	transient public boolean updated = false; //used for debugging

	void initialize() {
		mass = 1.0;
		number = 0;
	}

	Body() {
		//pos_x = pos_y = pos_z = 0.0;
		//vel_x = vel_y = vel_z = 0.0;
		initialize();
	}

	//used for sorting bodies, they're sorted using the 'number' field
	public int compareTo(Object o) {
		Body other = (Body) o;

		return this.number - other.number;
	}

	//copied from the rmi implementation
	//I used 'newAcc' instead of 'acc' to avoid confusion with this.acc
	public void computeNewPosition(boolean useOldAcc, double dt,
			double newAcc_x, double newAcc_y, double newAcc_z) {
		if (useOldAcc) {
			vel_x += (newAcc_x - oldAcc_x) * (dt / 2.0);
			vel_y += (newAcc_y - oldAcc_y) * (dt / 2.0);
			vel_z += (newAcc_z - oldAcc_z) * (dt / 2.0);
		}

		pos_x += (newAcc_x * (dt / 2.0) + vel_x) * dt;
		pos_y += (newAcc_y * (dt / 2.0) + vel_y) * dt;
		pos_z += (newAcc_z * (dt / 2.0) + vel_z) * dt;

		vel_x += newAcc_x * dt;
		vel_y += newAcc_y * dt;
		vel_z += newAcc_z * dt;

		//prepare for next call of BodyTreeNode.barnes()
		oldAcc_x = newAcc_x;
		oldAcc_y = newAcc_y;
		oldAcc_z = newAcc_z;
	}
}