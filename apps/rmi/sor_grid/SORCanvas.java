import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

class SORCanvas extends Canvas {
	private int width, height;
	private float[][] data;
	BufferedImage img = null;

	SORCanvas(int w, int h) {
		int i;

		//make it a little bigger so everything should fit
		setSize(w, h);

		width =  w;
		height = h;

		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
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

		for (int i = 0; i < height; i++) {
			for(int j = 0; j<width; j++) {
				int rgb = (int) (data[i][j] * 255); // one byte for red, one for green and one for blue
				if(rgb > 255 || rgb < 0) {
					System.err.println("rgb = " + rgb + ", val was " + data[i][j]);
					System.exit(1);
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
		this.data = data;
		repaint();
	}
}
