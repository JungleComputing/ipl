import java.io.*;

strictfp public final class Body implements Serializable, Cloneable,
											Comparable {

	//  public static int InstanceCount = 0;

	public int number;
	public Vec3 pos;
	public Vec3 vel;
	public Vec3 oldAcc;
	public double mass; 

	public Vec3 acc;

	/* this field counts the number of interactions with this body
	   were done in the *previous) iteration, we don't need it with satin */
	//public int weight; 

	void initialize() {
		pos = new Vec3();
		vel = new Vec3();
		//oldAcc = acc = null;
		mass = 1.0;
		number = 0;

		//InstanceCount++;
	}

	Body() {
		initialize();
	}

	Body(double x, double y, double z) {
		this();
		pos.x = x;
		pos.y = y;
		pos.z = z;
	}

	Body(double x, double y, double z, double vx, double vy, double vz) {
		this(x, y, z);
		vel.x = vx;
		vel.y = vy;
		vel.z = vz;
	}

	//used for sorting a list of bodies
	public int compareTo(Object o) {
		Body other = (Body) o;

		if (pos.compareTo(other.pos) != 0) {
			return pos.compareTo(other.pos);
		} else {
			return vel.compareTo(other.vel);
		}
	}


	//copied from the rmi implementation
	public void computeNewPosition(boolean useOldAcc, double dt, Vec3 acc) {
		Vec3 v;

		//System.out.println("acc: " + acc.x + ", " + oldAcc.x);

		if (useOldAcc) {
			v = new Vec3(acc); //vel += (acc-oldacc) * DT_HALF
			v.sub(oldAcc);
			v.mul(dt / 2.0);
			vel.add(v);
		}

		//System.out.println("vel: " + vel.x);

		v = new Vec3(acc); //pos += (acc * DT_HALF + vel) * DT
		v.mul(dt / 2.0);
		v.add(vel);
		v.mul(dt);
		pos.add(v);

		v = new Vec3(acc); //vel += acc * DT
		v.mul(dt);
		vel.add(v);

		//System.out.println("pos, vel: " + pos.x + ", " + vel.x);

		//prepare for next call of BodyTreeNode.barnes()
		oldAcc = acc;
		acc = new Vec3(0.0, 0.0, 0.0);

	}			
}
