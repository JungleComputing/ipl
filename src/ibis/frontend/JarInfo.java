/* $Id:$ */

package ibis.frontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Maintains information about the contents of a jarfile.
 */
public class JarInfo {

    /** Wether the contents of the jarfile is modified. */
    public boolean     modified;

    /**
     * A list of entries of the jarfile, including their contents.
     */
    public ArrayList<JarEntryInfo>  entries = new ArrayList<JarEntryInfo>();

    /** Reference to the jarfile itself. */
    public JarFile     jarFile;

    private class Enum implements Enumeration {
        int count = 0;

        public boolean hasMoreElements() {
            return count < entries.size();
        }

        public Object nextElement() {
            if (count >= entries.size()) {
                throw new NoSuchElementException();
            }
            return entries.get(count++);
        }
    }

    public JarInfo(JarFile jf) throws IOException {
        this.jarFile = jf;
        modified = false;
        
        for (Enumeration iitems = jf.entries(); iitems.hasMoreElements();) {
            JarEntry ient = (JarEntry) iitems.nextElement();
            entries.add(new JarEntryInfo(ient, this));
        }
    }

    public Enumeration entries() {
        return new Enum();
    }

    public void addEntry(IbiscEntry e) {
        JarEntry je = new JarEntry(e.fileName);
        JarEntryInfo jei = new JarEntryInfo(je, this, null);
        jei.ibiscEntry = e;
        entries.add(jei);
    }
}
