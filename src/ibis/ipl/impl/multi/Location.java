/* $Id: Location.java 7915 2008-04-08 13:32:45Z ceriel $ */

package ibis.ipl.impl.multi;

import ibis.ipl.IbisProperties;
import ibis.util.TypedProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;

/**
 * Represents a location on which an Ibis instance runs. This is the
 * data type returned by {@link MultiIbisIdentifier#location()}.
 * It represents a number of levels, for instance hostname, domain,
 * in that order, t.i., from detailed to coarse.
 */
public final class Location implements ibis.ipl.Location {

    private static final Location universe = new Location(new String[0]);

    private static final long serialVersionUID = 1L;

    /** Separates level names in a string representation of the location. */
    public static final String SEPARATOR = "@";

    /** The names of the levels. */
    private String levelNames[];

    /** Coded form. */
    private transient byte[] codedForm;

    private transient Location parent = null;

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
        if (s.equals("")) {
            levelNames = new String[0];
        } else {
            levelNames = s.split(SEPARATOR);
        }
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
     */
    public byte[] toBytes() {
        if (codedForm == null) {
            codedForm = computeCodedForm();
        }
        return codedForm.clone();
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
        if (codedForm == null) {
            codedForm = computeCodedForm();
        }
        dos.write(codedForm);
    }

    public int numberOfLevels() {
        return levelNames.length;
    }

    public String[] getLevels() {
        return levelNames.clone();
    }

    public String getLevel(int level) {
        return levelNames[level];
    }

    public int numberOfMatchingLevels(ibis.ipl.Location o) {
        int n1 = o.numberOfLevels();
        for (int i = levelNames.length-1; i >= 0; i--) {
            n1--;
            if (n1 < 0 || ! levelNames[i].equals(o.getLevel(n1))) {
                return levelNames.length-1-i;
            }
        }
        return levelNames.length;
    }

    public boolean equals(Object o) {
        if (! (o instanceof Location)) {
            return false;
        }
        Location l = (Location) o;
        if (l.levelNames.length != levelNames.length) {
            return false;
        }
        return numberOfMatchingLevels(l) == levelNames.length;
    }

    public int hashCode() {
        int retval = 0;
        for (int i = 0; i < levelNames.length; i++) {
            retval += levelNames[i].hashCode();
        }
        return retval;
    }

    /**
     * Method to retreive a default location, consisting of the domain and
     * hostname of this machine.
     * @param props properties.
     * @return the default location of this machine.
     */
    public static Location defaultLocation(Properties props) {
        TypedProperties p = new TypedProperties(props);
        String s = p.getProperty(IbisProperties.LOCATION);
        if (s != null) {
            return new Location(appendPostFix(p, s.split("@")));
        }

        try {
            InetAddress a = InetAddress.getLocalHost();
            s = a.getCanonicalHostName();
            if (s.length() > 0 && Character.isJavaIdentifierStart(s.charAt(0))) {
                return new Location(appendPostFix(p, s.split("\\.")));
            }

            String postFix =  p.getProperty(IbisProperties.LOCATION_POSTFIX);
            if(postFix == null) {
                return new Location(s);
            } else {
                return new Location(s + postFix);
            }
         } catch(IOException e) {
            return new Location("Unknown location");
        }
    }

    private static String[] appendPostFix(TypedProperties p, String[] components) {
        String postFix =  p.getProperty(IbisProperties.LOCATION_POSTFIX);
        if(postFix == null) {
            return components;
        }

        int count = components.length;
        String[] res = new String[count+1];
        for(int i=0;i<count; i++) {
            res[i] = components[i];
        }
        res[count] = postFix;
        return res;
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
        int n = o.numberOfLevels();
        for (int i = levelNames.length-1; i >= 0; i--) {
            n--;
            if (n < 0) {
                return 1;
            }

            int cmp = levelNames[i].compareTo(o.getLevel(n));
            if (cmp != 0) {
                return cmp;
            }
        }
        if (n == 0) {
            return 0;
        }
        return -1;
    }

    public ibis.ipl.Location getParent() {
        if (parent == null) {
            if (levelNames.length <= 1) {
                parent = universe;
            } else {
                String[] names = new String[levelNames.length-1];
                for (int i = 1; i < levelNames.length; i++) {
                    names[i-1] = levelNames[i];
                }
                parent = new Location(names);
            }
        }
        return parent;
    }

    public Iterator<String> iterator() {
        return new Iter();
    }

    private class Iter implements Iterator<String> {
        int index = levelNames.length;

        public boolean hasNext() {
            return index > 0;
        }

        public String next() {
            if (hasNext()) {
                return levelNames[--index];
            }
            throw new NoSuchElementException("Iterator exhausted");
        }

        public void remove() {
            throw new UnsupportedOperationException("remove() not supported");
        }
    }
}
