package ibis.impl.net;

//import ibis.ipl.Ibis;
import ibis.util.Timer;


/**
 * General purpose debugging message formatter.
 *
 * Note: the calling function name retrieval only works with JDK >= 1.4
 */
public final class NetLog {

        private static  final   boolean human           = false;


        private                 String  logName         = null;


        /**
         * Owner's name.
         */
        private                 String  moduleName      = null;

        /**
         * Active state flag.
         *
         * If set to <code>true</code>, debugging messages are printed; otherwise, messages are silently discarded.
         */
        private                 boolean on              = false;
        //static final private                 boolean on              = false;

        private static          Timer   timer           = null;

        {
                timer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");
        }


        /**
         * Constructor.
         *
         * @param on active state flag value.
         * @param moduleName logging object owner's name.
         */
        public NetLog(boolean on, String moduleName, String logName) {
                this.on         = on;
                this.moduleName = moduleName;
                this.logName    = logName;
        }

        private String _pre() {
                return human?"":logName+"{";
        }

        private String suf_() {
                return human?": ":"}"+logName+" ";
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
                        if (! ste.getClassName().equals(this.getClass().getName())) {
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

        /**
         * JDK >= 1.4 only
         */
        private final static java.util.regex.Pattern p = java.util.regex.Pattern.compile("^.*[.]([^.]+[.][^.]+)$");

        private String cleanFunctionName(String name) {
                java.util.regex.Matcher m = p.matcher(name);
                if (m.matches()) {
                        return m.group(1);
                }
                return name;
        }


        /*
         * To use with JDK < 1.4

        private String getLogId() {
                return moduleName+".<unknownMethod>";
        }

        private String getLogId(int frame) {
                return moduleName+".<unknownMethod>";
        }

        private String cleanFunctionName(String name) {
                return name;
        }

        */

        private String buildLocationString(StackTraceElement caller) {
                String s = "";

                s += caller.getClassName();
                s += ".";
                s += caller.getMethodName();
                s += "(";
                s += caller.getFileName();
                s += ":";
                s += caller.getLineNumber();
                s += ")";

                return s;
        }


        private String id(int f) {
                String s = "";
                StackTraceElement caller = getCaller(f + 1);

                if (!human) {
                        s += timer.currentTimeNanos();
                        s += "|";
                        s += Thread.currentThread().toString();
                        s += "|";
                        s += moduleName;
                        s += "|";
                        s += buildLocationString(caller);
                        s += "|";
                        s += Runtime.getRuntime().totalMemory()+"/"+Runtime.getRuntime().freeMemory()+"/"+Runtime.getRuntime().maxMemory();
                } else {
                        s = cleanFunctionName(caller.getClassName()+"."+caller.getMethodName());
                }

                return _pre() + s + suf_();
        }


        /**
         * Method entry point display.
         */
        public void in() {
                if (on) {
                        System.err.println(id(1)+"-->");
                }
        }

        /**
         * Method leave point display.
         */
        public void out() {
                if (on) {
                        System.err.println(id(1)+"<--");
                }
        }

        /**
         * Method entry point display.
         *
         * @param s some additional info string
         */
        public void in(String s) {
                if (on) {
                        System.err.println(id(1)+"-- "+s+" -->");
                }
        }

        /**
         * Method leave point display.
         *
         * @param s some additional info string
         */
        public void out(String s) {
                if (on) {
                        System.err.println(id(1)+"<-- "+s+" --");
                }
        }

        /**
         * Method entry point display.
         *
         * @param s some additional info string
         */
        public void in(String s, Object obj) {
                if (on) {
                        System.err.println(id(1)+"-- "+s + obj.toString() +" -->");
                }
        }

        /**
         * Method leave point display.
         *
         * @param s some additional info string
         */
        public void out(String s, Object obj) {
                if (on) {
                        System.err.println(id(1)+"<-- "+s + obj.toString() +" --");
                }
        }

        /**
         * General purpose method-prefixed message display.
         */
        public void disp(String s) {
                if (on) {
                        System.err.println(id(1)+"- "+s);
                }
        }

        /**
         * General purpose method-prefixed message display.
         */
        public void disp(Object s, Object obj) {
                if (on) {
                        System.err.println(id(1)+"- "+s + obj);
                }
        }

        /**
         * General purpose method-prefixed message display.
         */
        public void disp(Object s, Object obj, Object obj2) {
                if (on) {
                        System.err.println(id(1)+"- "+s + obj + obj2);
                }
        }

        /**
         * General purpose method-prefixed message display.
         */
        public void disp(Object s, int i) {
                if (on) {
                        System.err.println(id(1)+"- "+s + " " + i);
                }
        }

        /**
         * General purpose method-prefixed message display.
         */
        public void disp(Object s, Object obj, int i) {
                if (on) {
                        System.err.println(id(1)+"- "+s + obj + " " + i);
                }
        }

        /**
         * General purpose method-prefixed message display.
         */
        public void disp(Object s, int i, Object obj) {
                if (on) {
                        System.err.println(id(1)+"- "+s + " " + i + " " + obj);
                }
        }

        /**
         * General purpose method-prefixed message display.
         */
        public void disp(int j, Object s, int i) {
                if (on) {
                        System.err.println(id(1)+"- " + j + " " + s + " " + i);
                }
        }

        /**
         * General purpose method-prefixed message display.
         */
        public void disp(int j, Object s, int i, Object obj) {
                if (on) {
                        System.err.println(id(1)+"- " + j + " " + s + " " + i + " " + obj);
                }
        }

        public boolean on() {
                return on;
        }
}

