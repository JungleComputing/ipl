package ibis.ipl.impl.net;

public class NetEvent {
        private Object source = null;
        private int code = 0;
        private Object arg = null;

        public NetEvent(Object source, int code, Object arg) {
                this.source = source;
                this.code = code;
                this.arg = arg;
        }

        public NetEvent(Object source, int code) {
                this(source, code, null);
        }

        public Object source() {
                return source;
        }

        public int code() {
                return code;
        }

        public Object arg() {
                return arg;
        }
}
