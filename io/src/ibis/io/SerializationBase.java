/* $Id$ */

package ibis.io;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.util.Vector;

/**
 * A base class for all Ibis serialization classes, providing some
 * method implementations that they share.
 */
public class SerializationBase {
    /** 
     * Enable this to measure the time spent in serialization.
     * Each serialization entry/exit point must start/stop the timer.
     */
    protected final static boolean TIME_SERIALIZATION
            = IOProperties.properties.getBooleanProperty(IOProperties.s_timer, false);

    /** The serialization timer. */
    protected final SerializationTimer timer
        = TIME_SERIALIZATION ? new SerializationTimer(toString()) : null;

    private static Vector<SerializationTimer> timerList
            = new Vector<SerializationTimer>();

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

    SerializationBase() {
        initTimer();
    }

    public static void resetAllTimers() {
        synchronized (SerializationBase.class) {
            for (SerializationTimer t : timerList) {
                t.reset();
            }
        }
    }

    public static void printAllTimers() {
        synchronized (SerializationBase.class) {
            for (SerializationTimer t : timerList) {
                t.report();
            }
        }
    }

    private void initTimer() {
        if (TIME_SERIALIZATION) {
            synchronized (SerializationBase.class) {
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
     * Creates a {@link SerializationInput} as specified by the name.
     * @param name the nickname for this serialization type.
     * @param in   the underlying input stream.
     * @return the serialization input stream.
     */
    public static SerializationInput createSerializationInput(String name,
            DataInputStream in) throws IOException {
        String impl = implName(name) + "InputStream";
        try {
            Class<?> cl = Class.forName(impl);
            Constructor<?> cons =
                    cl.getConstructor(new Class[] {DataInputStream.class});
            return (SerializationInput) cons.newInstance(new Object[] {in});
        } catch(ClassNotFoundException e) {
            throw new IbisIOException("No such class: " + impl, e);
        } catch(NoSuchMethodException e) {
            throw new IbisIOException(
                    "No suitable constructor in class: " + impl, e);
        } catch(IllegalArgumentException e) {
            throw new IbisIOException(
                    "No suitable constructor in class: " + impl, e);
        } catch(InstantiationException e) {
            throw new IbisIOException("class " + impl + " is abstract", e);
        } catch(InvocationTargetException e) {
            throw new IbisIOException(
                    "constructor of " + impl + " threw an exception", e.getCause());
        } catch(IllegalAccessException e) {
            throw new IbisIOException(
                    "access to constructor of " + impl + " is denied", e);
        } catch(Throwable e) {
            throw new IbisIOException("got unexpected error", e);
        }
    }

    /**
     * Creates a {@link SerializationOutput} as specified by the name.
     * @param name the nickname for this serialization type.
     * @param out   the underlying output stream.
     * @return the serialization output stream.
     */
    public static SerializationOutput createSerializationOutput(String name,
            DataOutputStream out) throws IOException {
        String impl = implName(name) + "OutputStream";
        try {
            Class<?> cl = Class.forName(impl);
            Constructor<?> cons =
                    cl.getConstructor(new Class[] {DataOutputStream.class});
            return (SerializationOutput) cons.newInstance(new Object[] {out});
        } catch(ClassNotFoundException e) {
            throw new IbisIOException("No such class: " + impl, e);
        } catch(NoSuchMethodException e) {
            throw new IbisIOException(
                    "No suitable constructor in class: " + impl, e);
        } catch(IllegalArgumentException e) {
            throw new IbisIOException(
                    "No suitable constructor in class: " + impl, e);
        } catch(InstantiationException e) {
            throw new IbisIOException("class " + impl + " is abstract", e);
        } catch(InvocationTargetException e) {
            throw new IbisIOException(
                    "constructor of " + impl + " threw an exception", e.getCause());
        } catch(IllegalAccessException e) {
            throw new IbisIOException(
                    "access to constructor of " + impl + " is denied", e);
        } catch(Throwable e) {
            throw new IbisIOException("got unexpected error", e);
        }
    }
}
