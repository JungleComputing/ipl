/* $Id$ */

package ibis.io;

import ibis.ipl.Replacer;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * This abstract class is the interface provided by Ibis Serialization.
 * There are basically two ways to use this class:
 * <ul>
 * <li>Actually use <code>ObjectOutputStream</code>. In this case, the
 *     constructor version with an <code>OutputStream</code> parameter must
 *     be used. We call this the Sun serialization version.
 * <li>Redefine all of the <code>ObjectOutputStream</code>. In this case,
 *     the constructor without parameters must be used, and all methods of
 *     <code>ObjectOutputStream</code> must be redefined. This is the path
 *     taken when Ibis serialization is used.
 * </ul>
 */
public abstract class SerializationOutputStream extends ObjectOutputStream
        implements IbisStreamFlags {
    protected Replacer replacer;

    /**
     * Constructor which must be called for Ibis serialization.
     * The corresponding ObjectOutputStream constructor must be called,
     * so that all of the ObjectOutputStream methods can be redefined.
     *
     * @exception ibis.io.IOException is thrown on an IO error.
     */
    protected SerializationOutputStream() throws IOException {
        super();
        initTimer();
    }

    /**
     * Constructor which must be called for Sun serialization.
     *
     * @param s the <code>OutputStream</code> to be used
     * @exception ibis.io.IOException is thrown on an IO error.
     */
    protected SerializationOutputStream(OutputStream s) throws IOException {
        super(s);
        initTimer();
    }

    /** 
     * Enable this to measure the time spent in serialization.
     * Each serialization entry/exit point must start/stop the timer.
     */
    protected final static boolean TIME_SERIALIZATION
            = TypedProperties .booleanProperty(IOProps.s_timer, false)
              || TypedProperties.booleanProperty(IOProps.s_timer_out, false);

    /**
     * The serialization timer
     */
    protected final SerializationTimer timer
        = TIME_SERIALIZATION ? new SerializationTimer(toString()) : null;

    private static java.util.Vector timerList = new java.util.Vector();

    static {
        if (TIME_SERIALIZATION) {
            System.out.println("SerializationOutputStream.TIME_SERIALIZATION "
                    + "enabled");
            Runtime.getRuntime().addShutdownHook(
                    new Thread("SerializationOutputStream ShutdownHook") {
                        public void run() {
                            printAllTimers();
                        }
                    });
        }
    }

    public static void resetAllTimers() {
        synchronized (SerializationOutputStream.class) {
            java.util.Enumeration e = timerList.elements();
            while (e.hasMoreElements()) {
                SerializationTimer t = (SerializationTimer) e.nextElement();
                t.reset();
            }
        }
    }

    public static void printAllTimers() {
        synchronized (SerializationOutputStream.class) {
            java.util.Enumeration e = timerList.elements();
            while (e.hasMoreElements()) {
                SerializationTimer t = (SerializationTimer) e.nextElement();
                t.report();
            }
        }
    }

    private void initTimer() {
        if (TIME_SERIALIZATION) {
            synchronized (SerializationOutputStream.class) {
                timerList.add(timer);
            }
        }
    }

    protected final void startTimer() {
        if (TIME_SERIALIZATION) {
            timer.start();
        }
    }

    protected final void stopTimer() {
        if (TIME_SERIALIZATION) {
            timer.stop();
        }
    }

    protected final void suspendTimer() {
        if (TIME_SERIALIZATION) {
            timer.suspend();
        }
    }

    protected final void resumeTimer() {
        if (TIME_SERIALIZATION) {
            timer.resume();
        }
    }

    /**
     * Set a replacer. The replacement mechanism can be used to replace
     * an object with another object during serialization. This is used
     * in RMI, for instance, to replace a remote object with a stub. 
     * The replacement mechanism provided here is independent of the
     * serialization implementation (Ibis serialization, Sun
     * serialization).
     * 
     * @param replacer the replacer object to be associated with this
     *  output stream
     *
     * @exception java.io.IOException is thrown when enableReplaceObject
     *  throws an exception.
     */
    public void setReplacer(Replacer replacer) throws IOException {
        try {
            enableReplaceObject(true);
        } catch (Exception e) {
            // May throw a SecurityException.
            // Don't know how to deal with that.
            throw new IOException("enableReplaceObject threw exception: " + e);
        }
        this.replacer = replacer;
    }

    /**
     * Object replacement for Sun serialization. This method gets called by
     * Sun object serialization when replacement is enabled.
     *
     * @param obj the object to be replaced
     * @return the result of the object replacement
     */
    protected Object replaceObject(Object obj) {
        if (obj != null && replacer != null) {
            obj = replacer.replace(obj);
        }
        return obj;
    }

    /**
     * Returns the actual implementation used by the stream.
     *
     * @return the name of the actual serialization implementation used
     */
    public abstract String serializationImplName();

    /**
     * Write a slice of an array of booleans.
     * Warning: duplicates are NOT detected when these calls are used!
     *
     * @param ref the array to be written
     * @param off offset in the array from where writing starts
     * @param len the number of elements to be written
     *
     * @exception java.io.IOException is thrown on an IO error.
     */
    abstract public void writeArray(boolean[] ref, int off, int len)
            throws IOException;

    /**
     * Write a slice of an array of bytes.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(byte[] ref, int off, int len)
            throws IOException;

    /**
     * Write a slice of an array of shorts.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(short[] ref, int off, int len)
            throws IOException;

    /**
     * Write a slice of an array of chars.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(char[] ref, int off, int len)
            throws IOException;

    /**
     * Write a slice of an array of ints.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(int[] ref, int off, int len)
            throws IOException;

    /**
     * Write a slice of an array of longs.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(long[] ref, int off, int len)
            throws IOException;

    /**
     * Write a slice of an array of floats.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(float[] ref, int off, int len)
            throws IOException;

    /**
     * Write a slice of an array of doubles.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(double[] ref, int off, int len)
            throws IOException;

    /**
     * Write a slice of an array of objects.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(Object[] ref, int off, int len)
            throws IOException;

    /**
     * Write an array of booleans.
     *
     * @param ref the array to be written.
     * @exception java.io.IOException is thrown when an IO error occurs.
     */
    public void writeArray(boolean[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of bytes.
     *
     * @param ref the array to be written.
     * @exception java.io.IOException is thrown when an IO error occurs.
     */
    public void writeArray(byte[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of shorts.
     *
     * @param ref the array to be written.
     * @exception java.io.IOException is thrown when an IO error occurs.
     */
    public void writeArray(short[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of chars.
     *
     * @param ref the array to be written.
     * @exception java.io.IOException is thrown when an IO error occurs.
     */
    public void writeArray(char[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of ints.
     *
     * @param ref the array to be written.
     * @exception java.io.IOException is thrown when an IO error occurs.
     */
    public void writeArray(int[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of longs.
     *
     * @param ref the array to be written.
     * @exception java.io.IOException is thrown when an IO error occurs.
     */
    public void writeArray(long[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of floats.
     *
     * @param ref the array to be written.
     * @exception java.io.IOException is thrown when an IO error occurs.
     */
    public void writeArray(float[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of doubles.
     *
     * @param ref the array to be written.
     * @exception java.io.IOException is thrown when an IO error occurs.
     */
    public void writeArray(double[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of objects.
     *
     * @param ref the array to be written.
     * @exception java.io.IOException is thrown when an IO error occurs.
     */
    public void writeArray(Object[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    /**
     * Print some statistics. 
     */
    abstract public void statistics();
}
