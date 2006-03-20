/* $Id$ */

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

class BodyCanvas extends Canvas {

    // Using an image to draw the bodies does not flicker.
    private static final boolean USE_IMAGE = true;

    // Use continous scaling to keep all particales in the image.
    private static final boolean USE_SCALING = false;

    private static final int BORDER = 5;

    private int width, height;

    private Body[] bodies;

    private double maxx, maxy, minx, miny;

    private BufferedImage img;

    static BodyCanvas visualize(Body[] bodyArray) {
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        JFrame frame = new JFrame("Bodies");
        //frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        BodyCanvas bc = new BodyCanvas(500, 500, bodyArray);
        frame.getContentPane().add(bc);

        //Display the window.
        frame.pack();
        frame.setVisible(true);

        return bc;
    }

    private BodyCanvas(int w, int h, Body[] b) {
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
        computeBoundaries();
    }

    //find the maximum and minimum values of x and y
    private void computeBoundaries() {
        for (int i = 1; i < bodies.length; i++) {
            if (bodies[i] == null) {
                System.err.println("Warning: found null body in bodies array"
                        + " in BodyCanvas.paint()");
                continue;
            }
            maxx = Math.max(bodies[i].pos_x, maxx);
            maxy = Math.max(bodies[i].pos_y, maxy);
            minx = Math.min(bodies[i].pos_x, minx);
            miny = Math.min(bodies[i].pos_y, miny);
        }

        //        System.err.println("min x" + min.x + " min y " + min.y + "max x " +
        // max.x + " max y " +max.y);

        if (!USE_SCALING) {
            minx *= 20;
            miny *= 20;
            maxx = -minx;
            maxy = -miny;
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
                x = (int) ((bodies[i].pos_x - minx) / (maxx - minx) * (double) width)
                    + BORDER;
                y = (int) ((bodies[i].pos_y - miny) / (maxy - miny) * (double) height)
                    + BORDER;

                double col = bodies[i].vel_z;
                col *= 255;

                int color = 0;
                if(col < 0) { // moving away: red
                    col = Math.abs(col);
                    col = Math.min(255, col);
                    col = Math.max(80, col);
                    color = ((int) col) << 16;
                } else { // moving to the viewer: blue
                    col = Math.min(255, col);
                    col = Math.max(80, col);
                    color = ((int) col);
                }

                if (x > 0 && x < width && y > 0 && y < height) {
                    img.setRGB(x, y, color);
                }
            }

            g.drawImage(img, 0, 0, null);
        } else {
            g.clearRect(0, 0, width, height);

            g.setColor(Color.WHITE);

            for (i = 0; i < bodies.length; i++) {
                if (bodies[i] == null)
                    continue;
                x = (int) ((bodies[i].pos_x - minx) / (maxx - minx) * (double) width)
                    + BORDER;
                y = (int) ((bodies[i].pos_y - miny) / (maxy - miny) * (double) height)
                    + BORDER;
                if (x > 0 && x < width && y > 0 && y < height) {
                    g.drawLine(x, y, x, y);
                    //            g.fillOval(x, y, 1, 1);
                }
            }
        }
    }
}
