/* $Id$ */

package ibis.impl.nio;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

final class Debug {

    static final int TYPE_NAME_LENGTH = 20;

    static final int THREAD_NAME_LENGTH = 20;

    static final int SOURCE_NAME_LENGTH = 30;

    /**
     * Hashmap keeping track of level setting per thread
     */
    static HashMap lengths;

    static HashSet debugTypes;

    /**
     * are ALL debug types enabled?
     */
    static boolean all = false;

    static PrintStream log = null;

    static FileOutputStream file = null;

    static {
        lengths = new HashMap();

        debugTypes = new HashSet();

        String debugProp = System.getProperty(NioIbis.s_debug);

        if (debugProp != null) {
            StringTokenizer st = new StringTokenizer(debugProp, " ,\t\n\r\f");
            while (st.hasMoreTokens()) {
                String s = st.nextToken().toLowerCase();
                debugTypes.add(s);
            }
        }
    }

    static void setName(String name) throws IOException {
        if (System.getProperty(NioIbis.s_log) != null) {
            file = new FileOutputStream(name + ".log");
            log = new PrintStream(file);
        }
    }

    static void end() throws IOException {
        if (log != null) {
            log.close();
        }

        if (file != null) {
            file.close();
        }
    }

    private static boolean enabled(String type) {
        String negative = "-" + type;

        if (debugTypes.contains(negative)) {
            return false;
        } else if (debugTypes.contains(type)) {
            return true;
        } else if (debugTypes.contains("all")) {
            return true;
        } else {
            return false;
        }
    }

    private static int getLevel() {
        Integer level = (Integer) lengths.get(Thread.currentThread());

        if (level == null) {
            return 0;
        } else {
            return level.intValue();
        }
    }

    private static int incLevel() {
        int oldLevel = getLevel();

        lengths.put(Thread.currentThread(), new Integer(oldLevel + 1));

        return oldLevel;
    }

    private static int decLevel() {
        int oldLevel = getLevel();

        lengths.put(Thread.currentThread(), new Integer(oldLevel - 1));

        return oldLevel;
    }

    static synchronized void resetLevel() {
        lengths.remove(Thread.currentThread());
    }

    private static String spaces(int length) {
        String result = "";

        for (int i = 0; i < length; i++) {
            result = result + " ";
        }
        return result;
    }

    private static String pad(String string, int length) {
        if (string.length() > length) {
            return string.substring(string.length() - length);
        } else if (string.length() < length) {
            return string + spaces(length - string.length());
        } else { // string.length() == length
            return string;
        }
    }

    private static void printMessage(Object source, String type,
            String message, String seperator, int level) {
        String threadString = pad(Thread.currentThread().getName(),
                THREAD_NAME_LENGTH);
        String typeString = pad(type, TYPE_NAME_LENGTH);
        String sourceString;

        if (source == null) {
            sourceString = "<static>";
        } else {
            sourceString = source.getClass().getName();
        }

        sourceString = pad(sourceString, SOURCE_NAME_LENGTH);

        System.err.println(threadString + " " + typeString + " " + sourceString
                + " " + spaces(level * 2) + seperator + message);
    }

    private static void printLog(Object source, String type, String message,
            String seperator) {
        String sourceString;
        int spaces = 100 - message.length() - seperator.length();

        if (spaces > 0) {
            message = message + spaces(spaces);
        }

        if (source == null) {
            sourceString = "<static>";
        } else {
            sourceString = source.getClass().getName();
        }

        if (log != null) {
            log.println(seperator + message + " *** " + type + " * "
                    + Thread.currentThread().getName() + " * " + sourceString
                    + " ***");
        }
    }

    static synchronized void enter(String type, Object source, String message) {
        if (enabled(type)) {
            int level = incLevel();

            printMessage(source, type, message, "> ", level);
        }
        printLog(source, type, message, "> ");
    }

    static synchronized void message(String type, Object source, String message) {
        if (enabled(type)) {
            int level = getLevel();

            printMessage(source, type, message, "  ", level);
        }
        printLog(source, type, message, "");
    }

    static synchronized void exit(String type, Object source, String message) {
        if (enabled(type)) {
            int level = decLevel();

            printMessage(source, type, message, "< ", level - 1);
        }
        printLog(source, type, message, "< ");
    }
}