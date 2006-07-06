/* $Id:$ */

package ibis.frontend.ibis;

/**
 * This class maintains information about Java classes.
 */
class IbiscEntry {
    /** The Java class. */
    public ClassInfo   cl;

    /**
     * The name of the file from which the class was read (or the name of
     * the jar file entry).
     */
    public String      fileName;

    /**
     * Wether the entry was modified. Should be set to <code>true</code> by
     * any {@link ibis.frontend.ibis.IbiscComponent} that modifies this class.
     */
    public boolean     modified = false;

    /**
     * When the class comes from a jar file, information about this jar file
     * can be found here.
     */
    public JarInfo     jarInfo = null;

    /**
     * Constructs an <code>IbiscEntry</code> from the specified class and
     * filename.
     * @param cl the class.
     * @param fn the filename (or name of jar file entry).
     */
    public IbiscEntry(ClassInfo cl, String fn) {
        this.cl = cl;
        this.fileName = fn;
    }
}
