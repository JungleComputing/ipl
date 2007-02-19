/* $Id$ */

package ibis.ipl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

/**
 * Container for the capabilities of an {@link ibis.ipl.Ibis Ibis} or a
 * {@link ibis.ipl.PortType PortType}. There are boolean capabilities,
 * indicating that a certain capability is present, and there are
 * string-valued capabilities.
 * There are a number of predefined
 * capabilities, but Ibis implementations may add new ones.
 * A <code>Capabilities</code> object is immutable.
 */
public final class Capabilities implements java.io.Serializable {

    private transient byte[] codedForm;

    private final HashMap<String, String> stringCapabilities
            = new HashMap<String, String>();

    private final HashSet<String> booleanCapabilities = new HashSet<String>();

    /**
     * Constructs an empty capabilities object.
     */
    public Capabilities() {
        codedForm = computeCodedForm();
    }

    /**
     * Creates a capability object with the values specified in a
     * TypedProperties object.
     * @param sp the specified values.
     */
    public Capabilities(TypedProperties sp) {
        for (Enumeration e = sp.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            if (sp.booleanProperty(name)) {
                booleanCapabilities.add(name.toLowerCase());
            } else {
                stringCapabilities.put(name.toLowerCase(), 
                        sp.getProperty(name));
            }
        }

        codedForm = computeCodedForm();
    }

    public Capabilities(String[] values) {
        for (int i = 0; i < values.length; i++) {
            String s = values[i];
            int index = s.indexOf('=');
            if (index == -1) {
                booleanCapabilities.add(s.toLowerCase());
            } else {
                stringCapabilities.put(s.substring(0, index).toLowerCase(),
                        s.substring(index+1));
            }
        }
        codedForm = computeCodedForm();
    }

    /**
     * Creates a copy of a capability set.
     * @param c the capability set to copy.
     */
    public Capabilities(Capabilities c) {
        for (String cap : c.booleanCapabilities) {
            booleanCapabilities.add(cap);
        }
        for (String cap : c.stringCapabilities.keySet()) {
            stringCapabilities.put(cap, c.stringCapabilities.get(cap));
        }
        codedForm = c.codedForm;
    }

    /**
     * Creates a property set from a serialized form.
     * @param codedForm the serialized form, as produced by the
     * {@link #toBytes()} method.
     * @exception IOException is thrown in case of trouble.
     */
    public Capabilities(byte[] codedForm) throws IOException {
        this(codedForm, 0, codedForm.length);
    }

    /**
     * Creates a property set from a serialized form.
     * @param codedForm contains the serialized form, as produced by the
     * {@link #toBytes()} method.
     * @param offset offset where input for this method starts.
     * @param length length of input for this method.
     * @exception IOException is thrown in case of trouble.
     */
    public Capabilities(byte[] codedForm, int offset, int length)
            throws IOException {
        this(new DataInputStream(
                new ByteArrayInputStream(codedForm, offset, length)));
    }

    /**
     * Creates a property set by reading it from the specified input stream.
     * @param dis the input stream to read from.
     * @exception IOException is thrown in case of trouble.
     */
    public Capabilities(DataInput dis) throws IOException {
        doRead(dis);
        codedForm = computeCodedForm();
    }

    /**
     * Returns the property set represented as a byte array.
     * @return the byte array.
     */
    public byte[] toBytes() {
        // Not safe!! TODO: copy??? Or remove?
        return codedForm;
    }

    private byte[] computeCodedForm() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            doWrite(dos);
            dos.close();
            return bos.toByteArray();
        } catch(Exception e) {
            // Should not happen. Ignored.
            return new byte[0];
        }
    }

    /**
     * Writes the property set to the specified output stream.
     * @param dos the output stream to write to.
     * @exception IOException is thrown in case of trouble.
     */
    public void writeTo(DataOutput dos) throws IOException {
        dos.write(codedForm);
    }

    /**
     * Returns true if the specified capability is set.
     * @param cap the specified capability.
     * @return true if the capability is set.
     */
    public boolean hasCapability(String cap) {
        return booleanCapabilities.contains(cap.toLowerCase());
    }

    /**
     * Returns the string value of the specified capability, or null if not
     * present.
     * @param cap the specified capability name.
     * @return the value, or null.
     */
    public String getCapability(String cap) {
        return stringCapabilities.get(cap.toLowerCase());
    }

    /**
     * Matches the current capabilities with the capabilities
     * supplied. We have a match if the current capabilities are a subset
     * of the specified capabilities.
     * @param sp the capabilities to be matched with.
     * @return <code>true</code> if we have a match,
     * <code>false</code> otherwise.
     */
    public boolean matchCapabilities(Capabilities sp) {
        for (String cap : booleanCapabilities) {
            if (! sp.hasCapability(cap)) {
                return false;
            }
        }
        for (String cap : stringCapabilities.keySet()) {
            if (! stringCapabilities.get(cap).equals(
                        sp.stringCapabilities.get(cap))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Matches the current capabilities with the capabilities
     * supplied. Returns a capability object with the unmatched capabilities.
     * @param sp the capabilities to be matched with.
     * @return the capabilities that don't match.
     */
    public Capabilities unmatchedCapabilities(Capabilities sp) {
        Capabilities c = new Capabilities();
        for (String cap : booleanCapabilities) {
            if (! sp.hasCapability(cap)) {
                c.booleanCapabilities.add(cap);
            }
        }
        for (String cap : stringCapabilities.keySet()) {
            if (! stringCapabilities.get(cap).equals(
                        sp.stringCapabilities.get(cap))) {
                c.stringCapabilities.put(cap, stringCapabilities.get(cap));
            }
        }
        c.codedForm = computeCodedForm();
        return c;
    }

    /**
     * Computes and returns the intersection of this capability with the
     * specified capability.
     * @param sp the capabilities to intersect with.
     * @return the intersection.
     */
    public Capabilities intersect(Capabilities sp) {
        Capabilities c = new Capabilities();
        for (String cap : booleanCapabilities) {
            if (sp.hasCapability(cap)) {
                c.booleanCapabilities.add(cap);
            }
        }
        for (String cap : stringCapabilities.keySet()) {
            if (stringCapabilities.get(cap).equals(
                        sp.stringCapabilities.get(cap))) {
                c.stringCapabilities.put(cap, stringCapabilities.get(cap));
            }
        }
        c.codedForm = computeCodedForm();
        return c;
    }

    /**
     * Computes and returns the union of this capability with the
     * specified capability.
     * @param sp the capabilities to unite with.
     * @return the union.
     */
    public Capabilities unite(Capabilities sp) {
        Capabilities c = (Capabilities) sp.clone();
        for (String cap : booleanCapabilities) {
            if (! sp.hasCapability(cap)) {
                c.booleanCapabilities.add(cap);
            }
        }
        for (String cap : stringCapabilities.keySet()) {
            c.stringCapabilities.put(cap, stringCapabilities.get(cap));
        }
        c.codedForm = computeCodedForm();
        return c;
    }

    /**
     * Computes and returns the number of entries in this capability.
     * @return the number of entries.
     */
    public int size() {
        return booleanCapabilities.size() + stringCapabilities.size();
    }

    /**
     * Creates and returns a clone of this.
     * @return a clone.
     */
    public Object clone() {
        return new Capabilities(this);
    }

    /**
     * Reads and returns the capabilities from the specified file name, which is
     * searched for in the classpath.
     * @param name the file name.
     * @exception IOException is thrown when an IO error occurs.
     */
    public static Capabilities load(String name) throws IOException {
        InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(
                name);
        if (in == null) {
            throw new IOException("Could not open " + name);
        }
        return load(in);
    }

    /**
     * Reads and returns the capabilities from the specified input stream.
     * @param in the input stream.
     * @exception IOException is thrown when an IO error occurs.
     */
    public static Capabilities load(InputStream in) throws IOException {
        Properties p = new Properties();
        p.load(in);
        in.close();
        return new Capabilities(new TypedProperties(p));
    }

    /**
     * Returns <code>true</code> if <code>other</code> represents the same
     * capability set.
     * @param other the object to compare with.
     * @return <code>true</code> if equal, <code>false</code> otherwise.
     */
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof Capabilities)) {
            return false;
        }
        Capabilities o = (Capabilities) other;
        return booleanCapabilities.equals(o.booleanCapabilities)
            && stringCapabilities.equals(o.stringCapabilities);
    }

    /**
     * Returns the hashcode of this property set.
     * @return the hashcode.
     */
    public int hashCode() {
        return booleanCapabilities.hashCode();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        doWrite(out);
    }

    private void doWrite(DataOutput out) throws IOException {
        out.writeInt(booleanCapabilities.size());
        for (String cap : booleanCapabilities) {
            out.writeUTF(cap);
        }
        out.writeInt(stringCapabilities.size());
        for (String cap : stringCapabilities.keySet()) {
            out.writeUTF(cap);
            out.writeUTF(stringCapabilities.get(cap));
        }
    }

    private void readObject(ObjectInputStream in) throws IOException {
        doRead(in);
    }

    private void doRead(DataInput in) throws IOException {
        int sz = in.readInt();
        for (int i = 0; i < sz; i++) {
            String s = in.readUTF();
            booleanCapabilities.add(s);
        }
        sz = in.readInt();
        for (int i = 0; i < sz; i++) {
            String key = in.readUTF();
            String val = in.readUTF();
            stringCapabilities.put(key, val);
        }
        codedForm = computeCodedForm();
    }

    public String toString() {
        String str = "";
        for (String cap : booleanCapabilities) {
            str += cap + "\n";
        }
        for (String cap : stringCapabilities.keySet()) {
            str += cap + "=" + stringCapabilities.get(cap) + "\n";
        }
        return str;
    }
}
