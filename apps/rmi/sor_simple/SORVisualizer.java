import java.awt.*;
import java.util.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

class SORVisualizer {
	int width;
	int height;
	GlobalDataInterface global;
	static String masterName;

	SORVisualizer(int width, int height, GlobalDataInterface global) {
		this.width = width;
		this.height = height;
		this.global = global;
	}

	public void start() {
		SORCanvas c = createCanvas(width, height);
		float[][] data = null;

		try {
			// lookup remote sor object
			while (true) {
				try { 
					global = (GlobalDataInterface) Naming.lookup("//" + masterName + "/GlobalData");
					break;
				} catch (Exception e) {
					try {
						Thread.sleep(500);
					} catch (Exception foo) {}
				}
			}

			global.setRawDataSize(width, height);
		} catch (RemoteException e) {
			System.err.println("SORVisualizer.run: got exception: " + e);
			System.exit(1);
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

	private SORCanvas createCanvas( int width, int height )
        {

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
		GlobalDataInterface global = null;
		masterName = args[0];

		new SORVisualizer(500, 500, global).start();
		System.exit(0);
	}
}
