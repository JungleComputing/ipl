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
	public int weight; 


	Body() {
		initialize();
	}

	Body(double x, double y, double z) {
		this();
		pos.x = x;
		pos.y = y;
		pos.z = z;
	}

	void initialize() {

		pos = new Vec3();
		vel = new Vec3();
		acc = new Vec3();
		oldAcc = new Vec3();
		mass = 1.0;
		number = 0;

		//InstanceCount++;
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
