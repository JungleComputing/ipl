import java.awt.*;
import java.rmi.RemoteException;

class SORVisualizer {

    private int width;
    private int height;
    private float[][] data = null;
    private SORCanvas canvas;
    private boolean started = false;

    SORVisualizer(int width, int height, String[] args) {
	this.width = width;
	this.height = height;
	canvas = new SORCanvas(width, height);
	for (int i = 0; i < args.length; i++) {
	    new Reaper(args[i]).start();
	}

	System.out.println("Hit enter to start sampling");
	try {
	    System.in.read();
	} catch (java.io.IOException e) {
	    System.err.println("System.in.read() -> " + e);
	}
	synchronized (this) {
	    started = true;
	    notifyAll();
	}
    }

    private class Reaper extends Thread {

	private i_VisualBuffer visual;
	private String	masterName;

	Reaper(String masterName) {
	    this.masterName = masterName;

	    try {
		visual = (i_VisualBuffer)RMI_init.lookup("//" + masterName + "/VisualBuffer");
	    } catch (java.io.IOException e) {
		System.err.println("lookup fails " + e);
		System.exit(33);
	    }
	}

	public void run() {

	    try {
		visual.setRawDataSize(width, height);

		/* Wait for kickoff from user */
		synchronized (SORVisualizer.this) {
		    while (! started) {
			try {
			    wait();
			} catch (InterruptedException e) {
			    // have to live with this
			}
		    }
		}

		// do work
		while(true) {
System.err.print("[");
		    data = visual.getRawData();
System.err.print("]");
		    canvas.update(data); // give data to the canvas
		}

	    } catch (RemoteException e) {
		System.err.println("SORVisualizer.run: got exception: " + e);
	    }
	}

    }

    public static void main(String args[]) {
	new SORVisualizer(500, 500, args);
    }

}
