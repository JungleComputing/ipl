/* $Id$ */


import java.awt.*;
import java.rmi.RemoteException;

class SORVisualizer {

    private int width;

    private int height;

    private float[][] data = null;

    private SORCanvas canvas;

    private boolean started = false;

    private Barrier barrier;

    private final static boolean PAINT_SYNC = true;

    SORVisualizer(int width, int height, String[] args) {
        this.width = width;
        this.height = height;
        canvas = new SORCanvas(width, height);
        barrier = new Barrier(args.length);

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

    private class Barrier {

        private int hit;

        private int released;

        private final int n;

        Barrier(int n) {
            this.n = n;
            released = n;
            hit = 0;
        }

        private synchronized void syncAndRepaint() {
            hit++;
            if (hit == n) {
                canvas.repaint();
                released -= n;
                notifyAll();
            } else {
                while (hit != n) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
            released++;
            if (released == n) {
                hit -= n;
            }
        }

    }

    private class Reaper extends Thread {

        private i_VisualBuffer visual;

        private String masterName;

        Reaper(String masterName) {
            this.masterName = masterName;

            try {
                System.err.println("Try to locate //" + masterName
                        + "/VisualBuffer");
                visual = (i_VisualBuffer) RMI_init.lookup("//" + masterName
                        + "/VisualBuffer");
                System.err.println("located //" + masterName + "/VisualBuffer");
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
                    while (!started) {
                        try {
                            SORVisualizer.this.wait();
                        } catch (InterruptedException e) {
                            // have to live with this
                        }
                    }
                }

                // do work
                while (true) {
                    System.err.print("[");
                    data = visual.getRawData();
                    System.err.print("]");
                    canvas.update(data); // give data to the canvas
                    if (PAINT_SYNC) {
                        barrier.syncAndRepaint();
                    } else {
                        canvas.repaint();
                    }
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