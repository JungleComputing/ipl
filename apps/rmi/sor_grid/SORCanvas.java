import java.awt.*;
import java.awt.image.*;

class SORCanvas extends Canvas {

    private int width, height;
    private float[][] data;
    BufferedImage img = null;

    SORCanvas(int width, int height) {

	this.width =  width;
	this.height = height;

	// Create and set up the window.
	Frame frame = new Frame("SOR");
	//	    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	//	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	// make it a little bigger so everything should fit
	setSize(width, height);

	img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

	frame.add(this);
	frame.validate();

	data = new float[width][height];

	//Display the window.
	frame.pack();
	frame.setVisible(true);

	repaint();
    }

    /*
       public synchronized void paint(Graphics g) {
       if (data == null) return;

       for (int i = 0; i < height; i++) {
       for(int j = 0; j<width; j++) {
       g.setColor(new Color((float)data[i][j], (float)data[i][j], (float)data[i][j]));
       g.drawLine(i,j,i,j);
       }
       }
       }
       */

    public void update(Graphics g) {
	paint(g);
    }

    public synchronized void paint(Graphics g) {
	if (data == null) return;

	float min = Float.MAX_VALUE;
	float max = Float.MIN_VALUE;
	for (int i = 0; i < height; i++) {
	    for (int j = 0; j<width; j++) {
		if (data[i][j] > max) {
		    max = data[i][j];
		} else if (data[i][j] < min) {
		    min = data[i][j];
		}
	    }
	}

	float scale = 1.0F;
	if (min == Float.MIN_VALUE) {
	    min = max;
	} else {
	    scale = 255 / (max - min);
	}
	System.err.println("min " + min + " max " + max + " scale " + scale);

	for (int i = 0; i < height; i++) {
	    for(int j = 0; j<width; j++) {
		int rgb = (int) ((data[i][j] - min) * scale); // one byte for red, one for green and one for blue
		if(rgb > 255 || rgb < 0) {
		    System.err.println("rgb = " + rgb + ", val was " + data[i][j]);
		    if(rgb > 255) {
			rgb = 255;
		    } else {
			rgb = 0;
		    }
		    //					System.exit(1);
		}

		//				System.err.print(rgb + " ");
		img.setRGB(i, j, rgb);
	    }
	    //			System.err.println();
	}

	System.err.print(".");
	g.drawImage(img, 0, 0, null);
    }

    public synchronized void update(float[][] data) {
	for (int i = 0; i < data.length; i++) {
	    if (data[i] != null && data[i].length > 0) {
		System.arraycopy(data[i], 0,
			this.data[i], 0,
			data[i].length);
	    }
	}
    }

}
