import java.awt.*;
import java.rmi.RemoteException;

class SORVisualizer {
	int width;
	int height;
	i_GlobalData global;
	static String masterName;

	SORVisualizer(int width, int height, i_GlobalData global) {
		this.width = width;
		this.height = height;
		this.global = global;
	}

	public void start() {
		SORCanvas c = createCanvas(width, height);
		float[][] data = null;
		try {
			global = (i_GlobalData) RMI_init.lookup("//" + masterName + "/GlobalData");
		} catch (java.io.IOException e) {
			System.err.println("lookup fails " + e);
			System.exit(33);
		}

		// do work
		while(true) {
			try {
				data = global.getRawData();
			} catch (RemoteException e) {
				System.err.println("SORVisualizer.run: got exception: " + e);
				break;
			}

			c.update(data); // give data to the canvas
		}
	}

	private SORCanvas createCanvas(int width, int height) {

		//Create and set up the window.
		Frame frame = new Frame("SOR");
		//frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		SORCanvas c = new SORCanvas(width, height);
		frame.add(c);
		frame.validate();

		//Display the window.
		frame.pack();
		frame.setVisible(true);

		return c;
	}

	public static void main(String args[]) {
		i_GlobalData global = null;
		masterName = args[0];

		new SORVisualizer(500, 500, global).start();
		System.exit(0);
	}
}
