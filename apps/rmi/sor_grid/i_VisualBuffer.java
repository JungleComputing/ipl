/* $Id$ */


import java.rmi.Remote;
import java.rmi.RemoteException;

interface i_VisualBuffer extends Remote {

    /**
     * The visualizer sets the canvas size, so the workers know to what size
     * they must downsize for deployment. Waiting workers are notified.
     *
     * @param width canvas width in pixels
     * @param height canvas height in pixels
     */
    public void setRawDataSize(int width, int height) throws RemoteException;

    /**
     * Wait until the visualization module has deposit its frame size here
     * and return its width
     *
     * @return width of the canvas in pixels
     */
    public int getRawDataWidth() throws RemoteException;

    /**
     * Wait until the visualization module has deposit its frame size here
     * and return its height
     *
     * @return height of the canvas in pixels
     */
    public int getRawDataHeight() throws RemoteException;

    /**
     * Collect deposited data for display. Synchronizes according to
     * the value of {@link synchronous}.
     *
     * @return our slice of the downsized canvas
     */
    public float[][] getRawData() throws RemoteException;

    /**
     * Deploy our slice of the canvas
     *
     * @param m our canvas slice, that has been filled with {@link
     * 		downsample}
     */
    public void putMatrix(float[][] m) throws RemoteException;

}