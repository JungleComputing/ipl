// File: $Id$

/** Helper methods. */

final class Helpers {
    /** Protect constructor since it is a static only class. */
    private Helpers() {
    }

    /** Return a string describing the platform version that is used. */
    static String getPlatformVersion() {
        java.util.Properties p = System.getProperties();

        return "Java " + p.getProperty("java.version") + " ("
                + p.getProperty("java.vendor") + ") on "
                + p.getProperty("os.name") + " " + p.getProperty("os.version")
                + " (" + p.getProperty("os.arch") + ")";
    }
}