package ibis.ipl.impl.net;

public class NetPortEvent extends NetEvent {
        public static final int CLOSE_EVENT = 0;

        public NetPortEvent(Object source, int code, Object arg) {
                super(source, code, arg);
        }

        public NetPortEvent(Object source, int code) {
                super(source, code);
        }

}
