package ibis.ipl.impl.net;

/**
 * General purpose debugging message formatter.
 *
 * Note: the calling function name retrieval only works with JDK >= 1.4
 */
public final class NetLog {

        /**
         * Owner's name.
         */
        private String  moduleName = null;

        /**
         * Active state flag.
         *
         * If set to <code>true</code>, debugging messages are printed; otherwise, messages are silently discarded.
         */
        private boolean on         = false;

        /**
         * Constructor.
         *
         * @param on active state flag value.
         * @param moduleName logging object owner's name.
         */        
        public NetLog(boolean on, String moduleName) {
                this.moduleName = moduleName;
                this.on         = on;
        }
        
        /**
         * Constructor.
         *
         * @param on active state flag value.
         */        
        public NetLog(boolean on) {
                this(on, "unknownModule");
        }

        /**
         * General purpose message display.
         */
        public void log(String s) {
                if (on) {
                        System.err.println(moduleName+": "+s);
                }
        }
        

        /* JDK >= 1.4 only */

        /**
         * Generic caller frame retrieval.
         */
        private StackTraceElement getCaller() {
                StackTraceElement[] steArray = (new Throwable()).getStackTrace();
                int i = 1;
                
                while (i < steArray.length) {
                        StackTraceElement ste = steArray[i];
                        if (ste.getClassName() != this.getClass().getName()) {
                                return ste;
                        }

                        i++;
                }

                return null;
        }
        
        /**
         * Fixed caller frame retrieval.
         *
         * @param frame number of frames to skip.
         */
        private StackTraceElement getCaller(int frame) {
                frame++;
                StackTraceElement[] steArray = (new Throwable()).getStackTrace();
                
                if (frame < steArray.length) {
                        return steArray[frame];
                }

                return null;
        }
        
        /**
         *  Generic caller function name retrieval.
         */
        private String getLogId() {
                return getCaller().toString();
        }

        /**
         *  Fixed function name retrieval.
         *
         * @param frame number of frames to skip.
         */
        private String getLogId(int frame) {
                return getCaller(frame + 1).toString();
        }

        /*
         * To use with JDK < 1.4

        private String getLogId() {
                return moduleName+".<unknownMethod>";
        }
        
        private String getLogId(int frame) {
                return moduleName+".<unknownMethod>";
        }
        */

        /**
         * Method entry point display.
         */
        public void in() {
                if (on) {
                        System.err.println(getLogId(1)+": -->");
                }
        }
        
        /**
         * Method leave point display.
         */
        public void out() {
                if (on) {
                        System.err.println(getLogId(1)+": <--");
                }
        }
        
        /**
         * Method entry point display.
         *
         * @param s some additional info string
         */
        public void in(String s) {
                if (on) {
                        System.err.println(getLogId(1)+": -- "+s+" -->");
                }
        }
        
        /**
         * Method leave point display.
         *
         * @param s some additional info string
         */
        public void out(String s) {
                if (on) {
                        System.err.println(getLogId(1)+": <-- "+s+" --");
                }
        }
        
        /**
         * General purpose method-prefixed message display.
         */
        public void disp(String s) {
                if (on) {
                        System.err.println(getLogId(1)+": - "+s);
                }
        }
}

