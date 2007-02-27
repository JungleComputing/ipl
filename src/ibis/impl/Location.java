/* $Id :$ */

package ibis.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.net.InetAddress;

/**
 * Represents a location on which an Ibis instance runs. This is the
 * data type returned by {@link IbisIdentifier#getLocation()}.
 * It represents a number of levels, for instance domain, hostname,
 * in that order, t.i., from coarse to detailed.
 */
public final class Location implements ibis.ipl.Location {

    /** Separates level names in a string representation of the location. */
    public static final String SEPARATOR = "@";

    /** The names of the levels. */
    private String levelNames[];

    /** Coded form. */
    private final transient byte[] codedForm;

    /**
     * Constructs a location object from the specified level names.
     * @param levels the level names.
     */
    public Location(String[] levels) {
        levelNames = new String[levels.length];
        for (int i = 0; i < levels.length; i++) {
            levelNames[i] = levels[i];
        }
        codedForm = computeCodedForm();
    }

    /**
     * Constructs a location object from the specified string, in which
     * level names are separated by the separator character.
     * @param s the specified string.
     */
    public Location(String s) {
        levelNames = s.split(SEPARATOR);
        codedForm = computeCodedForm();
    }

    /**
     * Constructs a <code>Location</code> from the specified coded form.
     * @param codedForm the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public Location(byte[] codedForm) throws IOException {
        this(codedForm, 0, codedForm.length);
    }

    /**
     * Constructs a <code>Location</code> from the specified coded form,
     * at a particular offset and size.
     * @param codedForm the coded form.
     * @param offset offset in the coded form.
     * @param size size of the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public Location(byte[] codedForm, int offset, int size)
            throws IOException {
        this(new DataInputStream(
                new ByteArrayInputStream(codedForm, offset, size)));
    }

    /**
     * Reads a <code>Location</code> from the specified input stream.
     * @param dis the input stream.
     * @exception IOException is thrown in case of trouble.
     */
    public Location(DataInput dis) throws IOException {
        this(dis.readUTF());
    }

    /**
     * Returns the coded form of this <code>Location</code>.
     * @return the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public byte[] toBytes() {
        return (byte[]) codedForm.clone();
    }

    private byte[] computeCodedForm() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(toString());
            dos.close();
            return bos.toByteArray();
        } catch(Exception e) {
            // Should not happen. Ignore.
            return null;
        }
    }

    /**
     * Adds coded form of this <code>Location</code> to the specified
     * output stream.
     * @param dos the output stream.
     * @exception IOException is thrown in case of trouble.
     */
    public void writeTo(DataOutput dos) throws IOException {
        dos.write(codedForm);
    }

    public int numLevels() {
        return levelNames.length;
    }

    public String[] levels() {
        return levelNames.clone();
    }

    public String level(int level) {
        return levelNames[level];
    }

    public int matchingLevels(ibis.ipl.Location o) {
        int n = o.numLevels();
        for (int i = 0; i < levelNames.length; i++) {
            if (i >= n) {
                return i;
            }
            if (! levelNames[i].equals(o.level(i))) {
                return i;
            }
        }
        return levelNames.length;
    }

    public boolean equals(Object o) {
        if (! (o instanceof ibis.impl.Location)) {
            return false;
        }
        Location l = (Location) o;
        if (l.levelNames.length != levelNames.length) {
            return false;
        }
        return matchingLevels(l) == levelNames.length;
    }

    public int hashCode() {
        int retval = 0;
        for (int i = 0; i < levelNames.length; i++) {
            retval += levelNames[i].hashCode();
        }
        return retval;
    }

    public String cluster() {
        String retval = "";
        int n = levelNames.length - 1;
        for (int i = 0; i < n; i++) {
            if (i != 0) {
                retval += SEPARATOR;
            }
            retval += levelNames[i];
        }
        return retval;
    }

    public static Location defaultLocation() {
        try {
            InetAddress a = InetAddress.getLocalHost();
            String s = a.getCanonicalHostName();
            // What if a textual representation of an IP address is returned
            // here ???
            int index = s.lastIndexOf(".");
            if (index < 0) {
                return new Location(new String[] { s });
            }
            return new Location(new String[] {
                s.substring(0, index), s.substring(index+1)});
        } catch(IOException e) {
            return new Location("Unknown location");
        }
    }

    public String toString() {
        String retval = "";
        for (int i = 0; i < levelNames.length; i++) {
            if (i != 0) {
                retval += SEPARATOR;
            }
            retval += levelNames[i];
        }
        return retval;
    }

    public int compareTo(ibis.ipl.Location o) {
        int n = o.numLevels();
        for (int i = 0; i < levelNames.length; i++) {
            if (i >= n) {
                return 1;
            }
            int cmp = levelNames[i].compareTo(o.level(i));
            if (cmp != 0) {
                return cmp;
            }
        }
        if (n == levelNames.length) {
            return 0;
        }
        return -1;
    }

    public Iterator<String> iterator() {
        return new Iter();
    }

    private class Iter implements Iterator<String> {
        int index = 0;

        public boolean hasNext() {
            return index < levelNames.length;
        }

        public String next() {
            if (hasNext()) {
                return levelNames[index++];
            }
            throw new NoSuchElementException("Iterator exhausted");
        }

        public void remove() {
            throw new UnsupportedOperationException("remove() not supported");
        }
    }
}
