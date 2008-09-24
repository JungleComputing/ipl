/* $Id$ */

package ibis.io.jme;

import java.io.IOException;
import java.util.Vector;

/**
 * A base class for all Ibis serialization classes, providing some
 * method implementations that they share.
 */
public class SerializationBase extends IOProperties {
    /** 
     * Enable this to measure the time spent in serialization.
     * Each serialization entry/exit point must start/stop the timer.
     */
    protected final static boolean TIME_SERIALIZATION
            = properties.getBooleanProperty(s_timer, false);

    /** The serialization timer. */
    protected final SerializationTimer timer
        = TIME_SERIALIZATION ? new SerializationTimer(toString()) : null;

    private static Vector timerList
            = new Vector();

    static {
        if (TIME_SERIALIZATION) {
            System.out.println("SerializationOutputStream.TIME_SERIALIZATION "
                    + "enabled");
            /* TODO: Setup a shutdown system
            Runtime.getRuntime().addShutdownHook(
                    new Thread("SerializationOutputStream ShutdownHook") {
                        public void run() {
                            printAllTimers();
                        }
                    });
            */
        }
    }

    SerializationBase() {
        initTimer();
    }

    public static void resetAllTimers() {
        synchronized (SerializationBase.class) {
            for (int i = 0; i < timerList.size(); i++) {
            	SerializationTimer t = (SerializationTimer)timerList.elementAt(i);
                t.reset();
            }
        }
    }

    public static void printAllTimers() {
    	for (int i = 0; i < timerList.size(); i++) {
    		SerializationTimer t = (SerializationTimer)timerList.elementAt(i);	
    		t.report();	
    	}
    }

    private void initTimer() {
        if (TIME_SERIALIZATION) {
            synchronized (SerializationBase.class) {
                timerList.addElement(timer);
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
     * Returns the implementation name for the specified nickname.
     * For now, this is hardcoded, but it could be driven by for instance
     * a configuration or properties file.
     * @param name the nickname of the serialization type.
     * @return the implementation name.
     */
    private static String implName(String name) {
        if (name == null || name.equals("ibis") || name.equals("object")) {
            return "ibis.io.IbisSerialization";
        }
        if (name.equals("sun")) {
            return "ibis.io.SunSerialization";
        }
        if (name.equals("data")) {
            return "ibis.io.DataSerialization";
        }
        if (name.equals("byte")) {
            return "ibis.io.ByteSerialization";
        }
        return name;
    }

    /**
     * Creates a {@link ObjectInput} as specified by the name.
     * @param name the nickname for this serialization type.
     * @param in   the underlying input stream.
     * @return the serialization input stream.
     */
    public static ObjectInput createSerializationInput(String name,
            DataInputStream in) throws IOException {
        if (name == null || name.equals("jme")) {
            return new ObjectInputStream(in);
        }
        if (name.equals("data")) {
            return new DataSerializationInputStream(in);
        }
        if (name.equals("byte")) {
            return new ByteSerializationInputStream(in);
        }
        throw new SerializationError("Unknown serialization system: " + name);
    }

    /**
     * Creates a {@link ObjectOutput} as specified by the name.
     * @param name the nickname for this serialization type.
     * @param out   the underlying output stream.
     * @return the serialization output stream.
     */
    public static ObjectOutput createSerializationOutput(String name,
            DataOutputStream out) throws IOException {
        if (name == null || name.equals("jme")) {
            return new ObjectOutputStream(out);
        }
        if (name.equals("data")) {
            return new DataSerializationOutputStream(out);
        }
        if (name.equals("byte")) {
            return new ByteSerializationOutputStream(out);
        }
        throw new SerializationError("Unknown serialization system: " + name);
    }
}
