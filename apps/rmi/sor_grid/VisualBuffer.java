
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ibis.util.PoolInfo;

/**
 * Data buffer class for visualisation tool
 *
 * @author Rob van Nieuwpoort?
 * @author Rutger Hofman
 */
class VisualBuffer extends UnicastRemoteObject implements i_VisualBuffer {

    private int total_num;

    private int num_nodes;

    // for visualization
    private int width, height;

    private float[][] rawData;

    private boolean newDataAvailable = false;

    private boolean dataWritten = false;

    private boolean synchronous = false;

    private boolean doScaling = true;

    VisualBuffer(PoolInfo info, boolean synchronous) throws RemoteException {
        total_num = info.size();
        num_nodes = 0;
        this.synchronous = synchronous;
        System.err.println(this + ": in ctor");
        // Thread.dumpStack();
    }

    /**
     * The visualizer sets the canvas size, so the workers know to what size
     * they must downsize for deployment. Waiting workers are notified.
     *
     * @param width canvas width in pixels
     * @param height canvas height in pixels
     */
    public synchronized void setRawDataSize(int width, int height)
            throws RemoteException {
        System.err.println(this + ": setRawDataSize " + width + " x " + height);
        this.width = width;
        this.height = height;
        rawData = new float[height][width];
        notifyAll();
    }

    /**
     * Wait until the visualization module has deposit its frame size here
     * and return its width
     *
     * @return width of the canvas in pixels
     */
    public synchronized int getRawDataWidth() {
        while (rawData == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Go ahead waiting
            }
        }
        return width;
    }

    /**
     * Wait until the visualization module has deposit its frame size here
     * and return its height
     *
     * @return height of the canvas in pixels
     */
    public synchronized int getRawDataHeight() {
        while (rawData == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Go ahead waiting
            }
        }
        return height;
    }

    /**
     * Collect deposited data for display. Synchronizes according to
     * the value of {@link synchronous}.
     *
     * @return our slice of the downsized canvas
     */
    public synchronized float[][] getRawData() throws RemoteException {
        // never send the same data twice...
        System.err.println(this + ": attempt to collect RawData");
        while (synchronous && !dataWritten) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("eek: " + e);
            }
        }

        newDataAvailable = false;
        dataWritten = false;
        notifyAll();
        System.err.println(this + ": collected RawData");

        return rawData;
    }

    /**
     * Create a 2-D float array that corresponds to our slice of the data.
     *
     * @param m our slice of the data as an example. Unused rows are
     * 		<code>null</code> or length 0
     * @param width canvas width in pixels
     * @param height canvas height in pixels
     * @return downsized canvas
     */
    public static float[][] createDownsampledCanves(double[][] m, int width,
            int height) {
        // create the result matrix, downsample m.
        float[][] canvas = new float[height][];

        for (int i = 0; i < height; i++) {
            int ypos = i * m.length / height;
            if (m[ypos] != null && m[ypos].length > 0) {
                // System.err.println("Create canvas[" + i + "]");
                canvas[i] = new float[width];
            }
        }

        return canvas;
    }

    /**
     * Reduce the data to the size of the canvas and clip to floats to
     * reduce on the data rate with the visualizer
     *
     * @param m our slice of the data
     * @param canvas pre-allocated canvas, see {@link
     * 		createDownsampledCanvas}
     * @param width canvas width in pixels
     * @param height canvas height in pixels
     */
    public static void downsample(double[][] m, float[][] canvas, int width,
            int height) {
        for (int i = 0; i < height; i++) {
            int ypos = i * m.length / height;
            if (m[ypos] != null && m[ypos].length > 0) {
                // System.err.println("Downsample to canvas[" + i + "] from m[" + ypos + "]");
                for (int j = 0; j < width; j++) {
                    int xpos = j * m[ypos].length / width;

                    double[] row = m[ypos];
                    float val = (float) row[xpos];
                    canvas[i][j] = val;
                }
            }
        }
    }

    /**
     * Deploy our slice of the canvas
     *
     * @param m our canvas slice, that has been filled with {@link
     * 		downsample}
     */
    public synchronized void putMatrix(float[][] m) throws RemoteException {

        if (synchronous) {
            while (newDataAvailable) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    System.err.println("eek: " + e);
                }
            }
        } else {
            if (newDataAvailable)
                return;
        }

        System.arraycopy(m, 0, rawData, 0, m.length);
        for (int i = 0; i < height; i++) {
            if (m[i] != null) {
                System.arraycopy(m[i], 0, rawData[i], 0, m[i].length);
            }
        }

        dataWritten = true;
        notifyAll();
        // System.err.println(this + ": deposited matrix[" + height + "][" + width + "]");
    }

}