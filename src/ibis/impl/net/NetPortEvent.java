package ibis.impl.net;

/**
 * Provide a set of {@link ibis.impl.net.NetSendPort} {@link ibis.impl.net.NetReceivePort} state
 * events.
 */
public class NetPortEvent extends NetEvent {

        /**
         * Indicate that a connection has been asynchronously closed or lost.
         *
         * The connection identifier should be passed as an argument
         * along with this event.
         */
        public static final int CLOSE_EVENT = 0;

        /**
         * Construct an port event.
         */
        public NetPortEvent(Object source, int code, Object arg) {
                super(source, code, arg);
        }

        /**
         * Construct an port event.
         */
        public NetPortEvent(Object source, int code) {
                super(source, code);
        }

}
