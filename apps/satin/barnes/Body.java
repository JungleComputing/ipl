import java.io.*;
import javax.swing.*;

strictfp public final class Body implements Serializable, Cloneable {

	//  public static int InstanceCount = 0;

	public int number;
	public Vec3 pos;
	public Vec3 vel;
	public Vec3 acc;
	public Vec3 oldAcc;
	public double mass; 

	/* this field counts the number of interactions with this body
	   were done in the *previous) iteration, we don't need it with satin */
	//public int weight; 

	void initialize() {
		pos = new Vec3();
		vel = new Vec3();
		acc = new Vec3();
		oldAcc = new Vec3();
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

	//copied from the rmi implementation
	public void computeNewPosition(boolean useOldAcc, double dt) {
		Vec3 v;

		if (useOldAcc) {
			v = new Vec3(acc); //vel += (acc-oldacc) * DT_HALF
			v.sub(oldAcc);
			v.mul(dt / 2.0);
			vel.add(v);
		}

		v = new Vec3(acc); //pos += (acc * DT_HALF + vel) * DT
		v.mul(dt /2.0);
		v.add(vel);
		v.mul(dt);
		pos.add(v);

		v = new Vec3(acc); //vel += acc * DT
		v.mul(dt);
		vel.add(v);

		oldAcc = acc;

		//prepare for next call of BodyTreeNode.barnes()
		acc = new Vec3(0.0, 0.0, 0.0);
	}			

	static BodyCanvas visualizeArray(Body[] bodies) {
		JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        JFrame frame = new JFrame("Bodies");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

 		BodyCanvas bc = new BodyCanvas(500, 500, bodies);
		frame.getContentPane().add(bc);

        //Display the window.
        frame.pack();
        frame.setVisible(true);

		return bc;
	}
}
