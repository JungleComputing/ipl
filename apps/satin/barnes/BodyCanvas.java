/* $Id$ */


import java.awt.*;
//import javax.swing.*;
import java.awt.event.*;

class BodyCanvas extends Canvas {
    private static final int BORDER = 5;

    private int width, height;

    private Body[] bodies;

    private Vec3 max, min;

    BodyCanvas(int w, int h, Body[] b) {
        int i;

        if (b[0] == null) {
            System.err.println("EEK: bodies[0] == null in BodyCanvas");
            System.exit(1);
        }

        //make it a little bigger so everything should fit
        setSize(w + BORDER * 2, h + BORDER * 2);

        width = w;
        height = h;
        bodies = b;

        //find the maximum and minimum values of x and y
        max = new Vec3(bodies[0].pos);
        min = new Vec3(bodies[0].pos);

        for (i = 1; i < bodies.length; i++) {
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
    }

    public synchronized void paint(Graphics g) {
        int i, x, y;

        for (i = 0; i < bodies.length; i++) {
            if (bodies[i] == null)
                continue;
            x = (int) ((bodies[i].pos.x - min.x) / (max.x - min.x) * (double) width)
                    + BORDER;
            y = (int) ((bodies[i].pos.y - min.y) / (max.y - min.y) * (double) height)
                    + BORDER;
            //g.drawLine(x,y,x,y);
            g.drawOval(x, y, 3, 3);
        }
    }

    public synchronized void setBodies(Body[] bodies) {
        this.bodies = bodies;
        repaint();
    }

}