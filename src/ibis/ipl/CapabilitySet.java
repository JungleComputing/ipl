/* $Id$ */

package ibis.ipl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

/**
 * Container for the capabilities of an {@link ibis.ipl.Ibis Ibis} or a
 * representation of a port type. There are boolean capabilities,
 * indicating that a certain capability is present, and there are
 * string-valued capabilities.
 * There are a number of predefined
 * capabilities, but Ibis implementations may add new ones.
 * A <code>CapabilitySet</code> object is immutable.
 */
class CapabilitySet {

    private transient byte[] codedForm;

    private final HashMap<String, String> stringCapabilities
            = new HashMap<String, String>();

    private final HashSet<String> booleanCapabilities = new HashSet<String>();

    /**
     * Constructs an empty capabilities object.
     */
    public CapabilitySet() {
        codedForm = computeCodedForm();
    }

    /**
     * Creates a capability set with the specified values.
     * @param values the specified values.
     */
    public CapabilitySet(String... values) {
        for (String value : values) {
            int index = value.indexOf('=');
            if (index == -1) {
                booleanCapabilities.add(value.toLowerCase());
            } else {
                stringCapabilities.put(value.substring(0, index).toLowerCase(),
                        value.substring(index+1));
            }
        }
        codedForm = computeCodedForm();
    }

    /**
     * Creates a copy of a capability set.
     * @param capabilitySet the capability set to copy.
     */
    public CapabilitySet(CapabilitySet capabilitySet) {
        for (String capability : capabilitySet.booleanCapabilities) {
            booleanCapabilities.add(capability);
        }
        for (String capability : capabilitySet.stringCapabilities.keySet()) {
            stringCapabilities.put(capability, capabilitySet.stringCapabilities.get(capability));
        }
        codedForm = capabilitySet.codedForm;
    }

    /**
     * Creates a capability set with the values specified in a
     * Properties object.
     * @param properties the specified values.
     */
    protected CapabilitySet(Properties properties) {
        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            String value = properties.getProperty(name);
            if (value != null
                    && (value.equals("1") || value.equals("on")
                        || value.equals("") || value.equals("true")
                        || value.equals("yes"))) {
                booleanCapabilities.add(name.toLowerCase());
            } else {
                stringCapabilities.put(name.toLowerCase(), value);
            }
        }

        codedForm = computeCodedForm();
    }

    /**
     * Creates a property set from a serialized form.
     * @param codedForm the serialized form, as produced by the
     * {@link #toBytes()} method.
     * @exception IOException is thrown in case of trouble.
     */
    public CapabilitySet(byte[] codedForm) throws IOException {
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
    public CapabilitySet(byte[] codedForm, int offset, int length)
            throws IOException {
        this(new DataInputStream(
                new ByteArrayInputStream(codedForm, offset, length)));
    }

    /**
     * Creates a property set by reading it from the specified input stream.
     * @param dis the input stream to read from.
     * @exception IOException is thrown in case of trouble.
     */
    public CapabilitySet(DataInput dis) throws IOException {
        doRead(dis);
        codedForm = computeCodedForm();
    }

    /**
     * Returns the property set represented as a byte array.
     * @return the byte array.
     */
    public byte[] toBytes() {
        return (byte[]) codedForm.clone();
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
     * @param dataOutput the output stream to write to.
     * @exception IOException is thrown in case of trouble.
     */
    public void writeTo(DataOutput dataOutput) throws IOException {
        dataOutput.write(codedForm);
    }

    /**
     * Returns true if the specified boolean capability is set.
     * @param capability the specified capability.
     * @return true if the capability is set.
     */
    public boolean hasCapability(String capability) {
        return booleanCapabilities.contains(capability.toLowerCase());
    }

    /**
     * Returns the string value of the specified capability, or null if not
     * present.
     * @param capability the specified capability name.
     * @return the value, or null.
     */
    public String getCapability(String capability) {
        return stringCapabilities.get(capability.toLowerCase());
    }

    /**
     * Matches the current capabilities with the capabilities
     * supplied. We have a match if the current capabilities are a subset
     * of the specified capabilities.
     * @param capabilitySet the capabilities to be matched with.
     * @return <code>true</code> if we have a match,
     * <code>false</code> otherwise.
     */
    public boolean matchCapabilities(CapabilitySet capabilitySet) {
        for (String capability : booleanCapabilities) {
            if (! capabilitySet.hasCapability(capability)) {
                return false;
            }
        }
        for (String capability : stringCapabilities.keySet()) {
               if (! capabilitySet.stringCapabilities.keySet().contains(capability)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Matches the current capabilities with the capabilities
     * supplied. Returns a capability set with the unmatched capabilities.
     * @param capabilitySet the capabilities to be matched with.
     * @return the capabilities that don't match.
     */
    public CapabilitySet unmatchedCapabilities(CapabilitySet capabilitySet) {
        CapabilitySet result = new CapabilitySet();
        for (String capability : booleanCapabilities) {
            if (! capabilitySet.hasCapability(capability)) {
                result.booleanCapabilities.add(capability);
            }
        }
        for (String capability : stringCapabilities.keySet()) {
            if (! stringCapabilities.get(capability).equals(
                        capabilitySet.stringCapabilities.get(capability))) {
                result.stringCapabilities.put(capability, stringCapabilities.get(capability));
            }
        }
        result.codedForm = computeCodedForm();
        return result;
    }

    /**
     * Computes and returns the number of entries in this capability set.
     * @return the number of entries.
     */
    public int size() {
        return booleanCapabilities.size() + stringCapabilities.size();
    }

    /**
     * Reads and returns the capabilities from the specified file name, which is
     * searched for in the classpath.
     * @param name the file name.
     * @return the capabilities from the specified file.
     * @exception IOException is thrown when an IO error occurs.
     */
    public static CapabilitySet load(String name) throws IOException {
        InputStream in
            = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
        if (in == null) {
            throw new IOException("Could not open " + name);
        }
        return load(in);
    }

    /**
     * Reads and returns the capabilities from the specified input stream.
     * @param input the input stream.
     * @return the capabilities from the specified input stream.
     * @exception IOException is thrown when an IO error occurs.
     */
    public static CapabilitySet load(InputStream input) throws IOException {
        Properties properties = new Properties();
        properties.load(input);
        input.close();
        return new CapabilitySet(properties);
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
        if (!(other instanceof CapabilitySet)) {
            return false;
        }

        CapabilitySet capabilitySet = (CapabilitySet) other;
        return booleanCapabilities.equals(capabilitySet.booleanCapabilities)
            && stringCapabilities.equals(capabilitySet.stringCapabilities);
    }

    /**
     * Returns the hashcode of this property set.
     * @return the hashcode.
     */
    public int hashCode() {
        return booleanCapabilities.hashCode();
    }

    private void writeObject(ObjectOutputStream output) throws IOException {
        doWrite(output);
    }

    private void doWrite(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(booleanCapabilities.size());
        for (String capability : booleanCapabilities) {
            dataOutput.writeUTF(capability);
        }
        dataOutput.writeInt(stringCapabilities.size());
        for (String capability : stringCapabilities.keySet()) {
            dataOutput.writeUTF(capability);
            dataOutput.writeUTF(stringCapabilities.get(capability));
        }
    }

    private void readObject(ObjectInputStream input) throws IOException {
        doRead(input);
    }

    private void doRead(DataInput dataInput) throws IOException {
        int size = dataInput.readInt();
        for (int i = 0; i < size; i++) {
            booleanCapabilities.add(dataInput.readUTF());
        }
        size = dataInput.readInt();
        for (int i = 0; i < size; i++) {
            String key = dataInput.readUTF();
            String val = dataInput.readUTF();
            stringCapabilities.put(key, val);
        }
        codedForm = computeCodedForm();
    }

    public String toString() {
        String result = "";
        for (String capability : booleanCapabilities) {
            result += capability + "\n";
        }
        for (String capability : stringCapabilities.keySet()) {
            result += capability + "=" + stringCapabilities.get(capability) + "\n";
        }
        return result;
    }

    /**
     * Returns all the capabilities in this capability set as a String
     * array, one element per capability.
     * @return the capabilities.
     */
    public String[] getCapabilities() {
        int size = booleanCapabilities.size() + stringCapabilities.size();
        String[] result = new String[size];
        int i = 0;
        for (String capability : booleanCapabilities) {
            result[i] = capability;
            i++;
        }
        for (String capability : stringCapabilities.keySet()) {
            result[i] = capability + "=" + stringCapabilities.get(capability);
            i++;
        }
        return result;
    }
}
