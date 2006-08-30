/* $Id$ */

package ibis.impl.net;

/**
 * Provide an abstract class for implementing {@linkplain
 * NetEventQueue event-queue} elements.
 */
public abstract class NetEvent {

    /**
     * Reference the sender of the event.
     */
    private Object source = null;

    /**
     * Store the numerical code of the event.
     */
    private int code = 0;

    /**
     * Store an optional argument associated to the event.
     */
    private Object arg = null;

    /**
     * Construct an event.
     *
     * @param source the sender of the event.
     * @param code the numerical code of the event.
     * @param arg the optional argument associated to the event.
     */
    public NetEvent(Object source, int code, Object arg) {
        this.source = source;
        this.code = code;
        this.arg = arg;
    }

    /**
     * Construct an event without an argument.
     *
     * @param source the sender of the event.
     * @param code the numerical code of the event.
     */
    public NetEvent(Object source, int code) {
        this(source, code, null);
    }

    /**
     * Return a reference to the sender of the event.
     * @return a reference to the sender of the event.
     */
    public Object source() {
        return source;
    }

    /**
     * Return the code of the event.
     * @return the code of the event.
     */
    public int code() {
        return code;
    }

    /**
     * Return the optional argument of the event or null if no
     * argument is associated to this event.
     *
     * @return the optional argument of the event or null if no
     * argument is associated to this event.
     */
    public Object arg() {
        return arg;
    }
}
