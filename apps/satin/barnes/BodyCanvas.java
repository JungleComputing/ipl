/* $Id$ */

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

class BodyCanvas extends Canvas {

	// Using an image to draw the bodies does not flicker.
	private static final boolean USE_IMAGE = true;

	// Use continous scaling to keep all particales in the image.
	private static final boolean USE_SCALING = false;

	private static final int BORDER = 5;

	private int width, height;

	private Body[] bodies;

	private Vec3 max, min;

	private BufferedImage img;

	BodyCanvas(int w, int h, Body[] b) {
		width = w;
		height = h;
		bodies = b;

		if (b[0] == null) {
			System.err.println("EEK: bodies[0] == null in BodyCanvas");
			System.exit(1);
		}

		//make it a little bigger so everything should fit
		setSize(w + BORDER * 2, h + BORDER * 2);

		if (USE_IMAGE) {
			img = new BufferedImage(w + BORDER * 2, h + BORDER * 2,
					BufferedImage.TYPE_INT_RGB);
		}

		setBackground(Color.BLACK);

		//find the maximum and minimum values of x and y
		max = new Vec3(bodies[0].pos);
		min = new Vec3(bodies[0].pos);

		computeBoundaries();
	}

	private void computeBoundaries() {
		for (int i = 1; i < bodies.length; i++) {
			if (bodies[i] == null) {
				System.err.println("Warning: found null body in bodies array"
						+ " in BodyCanvas.paint()");
				continue;
			}
			max.x = Math.max(bodies[i].pos.x, max.x);
			max.y = Math.max(bodies[i].pos.y, max.y);
			min.x = Math.min(bodies[i].pos.x, min.x);
			min.y = Math.min(bodies[i].pos.y, min.y);
		}

		//        System.err.println("min x" + min.x + " min y " + min.y + "max x " +
		// max.x + " max y " +max.y);

		if (!USE_SCALING) {
			min.x *= 20;
			min.y *= 20;
			max.x = -min.x;
			max.y = -min.y;
		}
	}

	public void update(Graphics g) {
		paint(g);
	}

	public synchronized void paint(Graphics g) {
		int i, x, y;

		if (USE_SCALING) {
			computeBoundaries();
		}

		if (USE_IMAGE) {
			// clear image
			for (int a = 0; a < height; a++) {
				for (int b = 0; b < width; b++) {
					img.setRGB(b, a, 0);
				}
			}

			for (i = 0; i < bodies.length; i++) {
				if (bodies[i] == null)
					continue;
				x = (int) ((bodies[i].pos.x - min.x) / (max.x - min.x) * (double) width)
						+ BORDER;
				y = (int) ((bodies[i].pos.y - min.y) / (max.y - min.y) * (double) height)
						+ BORDER;

				if (x > 0 && x < width && y > 0 && y < height) {
					img.setRGB(x, y, 0xffffffff);
				}
			}

			g.drawImage(img, 0, 0, null);
		} else {
			g.clearRect(0, 0, width, height);

			g.setColor(Color.WHITE);

			for (i = 0; i < bodies.length; i++) {
				if (bodies[i] == null)
					continue;
				x = (int) ((bodies[i].pos.x - min.x) / (max.x - min.x) * (double) width)
						+ BORDER;
				y = (int) ((bodies[i].pos.y - min.y) / (max.y - min.y) * (double) height)
						+ BORDER;
				if (x > 0 && x < width && y > 0 && y < height) {
					g.drawLine(x, y, x, y);
					//            g.fillOval(x, y, 1, 1);
				}
			}
		}
	}
}