package ibis.ipl.impl.net;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.Hashtable;

public abstract class NetStat {
        private static String dummy = Runtime.getRuntime().toString();

        protected String  moduleName = null;
        protected boolean on         = false;
        protected Hashtable pluralExceptions = new Hashtable();

        private Thread reportThread  = null;

        public NetStat(boolean on, String moduleName) {
                this.moduleName = moduleName;
                this.on         = on;

                if (on) {
                        Runtime.getRuntime().addShutdownHook(new Thread() {
			        public void run() {
                                        synchronized (dummy) {
                                                report();
                                        }
			        }
		        });
                }
        }

        public NetStat(boolean on) {
                this(on, "");

        }

        public abstract void report();

        /**
         * Returns plural of <code>word</code> if <code>value</code> > 1.
         *
         * Note plural exceptions should be put in the table {@link #pluralExceptions} by subclasses.
         *
         * @param word the word to process.
         * @param value the value to consider.
         */
        protected String plural(String word, long value) {
                if (value > 1) {
                        String s = (String)pluralExceptions.get(word);
                        if (s == null) {
                                s = word+"s";
                        }

                        return s;
                } else {
                        return word;
                }
        }

        /**
         * Returns plural of <code>word</code> if <code>value</code> > 1.
         *
         * Note plural exceptions should be put in the table {@link #pluralExceptions} by subclasses.
         *
         * @param word the word to process.
         * @param value the value to consider.
         */
        protected String plural(String word, Number value) {
                if (value.longValue() > 1) {
                        String s = (String)pluralExceptions.get(word);
                        if (s == null) {
                                s = word+"s";
                        }

                        return s;
                } else {
                        return word;
                }
        }

        /**
         * Report 'a quantity of something'.
         *
         * @param value the quantity.
         * @param String the 'name' of 'something'.
         */
        protected void reportVal(long value, String type) {
                if (value < 1) {
                        return;
                }

                System.err.println(value + " " + plural(type, value));
        }

        /**
         * Report 'a quantity of something'.
         *
         * Note: if <code>value</code> is lower than 1, nothing is displayed.
         *
         * @param value the quantity.
         * @param type the singular 'name' of 'something' (e.g. 'byte', 'message').
         */
        protected void reportVal(Number value, String type) {
                if (value.longValue() < 1) {
                        return;
                }

                System.err.println(value + " " + plural(type, value));
        }

        /**
         * Report 'a quantity of something'.
         *
         * Note: if <code>value</code> is lower than 1, nothing is displayed.
         *
         * @param value the quantity.
         * @param begin beginning of sentence (printed after the value).
         * @param type the singular 'name' of 'something' (e.g. 'byte', 'message').
         * @param end end of sentence.
         */
        protected void reportVal(Number value,  String begin, String type, String end) {
                if (value.longValue() < 1) {
                        return;
                }

                System.err.println(value + " " + begin+ " " + plural(type, value) + " " + end);
        }

        /**
         * Report 'a quantity of something'.
         *
         * Note: if <code>value</code> is lower than 1, nothing is displayed.
         *
         * @param value the quantity.
         * @param begin beginning of sentence (printed after the value).
         * @param type the singular 'name' of 'something' (e.g. 'byte', 'message').
         * @param end end of sentence.
         */
        protected void reportVal(long value,  String begin, String type, String end) {
                if (value < 1) {
                        return;
                }

                System.err.println(value + " " + begin+ " " + plural(type, value) + " " + end);
        }

        /**
         * Report 'a set of quantities of something'.
         * Note: if <code>m.size()</code> is lower than 1, nothing is displayed.
         *
         * @param m the tree map of quantities.
         * @param setType the 'name' of a 'set' of something (e.g. 'array', 'buffer').
         * @param type the 'name' of 'something' (e.g. 'byte', 'message').
         */
        protected void reportMap(TreeMap m, String setType, String type) {
                if (m.size() < 1) {
                        return;
                }

                System.err.println(" - "+
                                   plural(setType, m.size())+
                                   " of "+
                                   plural(type, 2)+
                                   " -");
                Iterator i = m.keySet().iterator();

                while (i.hasNext()) {
                        Integer key = (Integer)i.next();
                        Integer nb  = (Integer)m.get(key);

                        System.err.println(nb +" "+plural(setType, nb)+
                                           " of "+
                                           key+" "+plural(type, key));
                }
        }

}
