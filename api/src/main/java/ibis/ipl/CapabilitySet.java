/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.util.HashSet;
import java.util.Properties;

/**
 * Container for the capabilities of an {@link ibis.ipl.Ibis Ibis} or a
 * representation of a port type. These are represented as boolean capabilities,
 * its presence indicating that a certain capability is present. There are a
 * number of predefined capabilities, but Ibis implementations may add new ones.
 * A <code>CapabilitySet</code> object is immutable.
 */
public class CapabilitySet {

    private transient byte[] codedForm;

    private final HashSet<String> capabilities = new HashSet<>();

    /**
     * Constructs an empty capabilities object.
     */
    public CapabilitySet() {
        codedForm = computeCodedForm();
    }

    /**
     * Creates a capability set with the specified values.
     *
     * @param values the specified values.
     */
    public CapabilitySet(String... values) {
        for (String value : values) {
            capabilities.add(value.toLowerCase());
        }
        codedForm = computeCodedForm();
    }

    /**
     * Creates a copy of a capability set.
     *
     * @param capabilitySet the capability set to copy.
     */
    public CapabilitySet(CapabilitySet capabilitySet) {
        capabilities.addAll(capabilitySet.capabilities);
        codedForm = capabilitySet.codedForm;
    }

    /**
     * Creates a capability set with the values specified in a Properties object.
     *
     * @param properties the specified values.
     */
    @SuppressWarnings("unchecked")
    protected CapabilitySet(Properties properties) {
        for (Enumeration<String> e = (Enumeration<String>) properties.propertyNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            String value = properties.getProperty(name);
            if (value != null && (value.equals("1") || value.equals("on") || value.equals("") || value.equals("true") || value.equals("yes"))) {
                capabilities.add(name.toLowerCase());
            }
        }

        codedForm = computeCodedForm();
    }

    /**
     * Creates a capability set from a serialized form.
     *
     * @param codedForm the serialized form, as produced by the {@link #toBytes()}
     *                  method.
     * @exception IOException is thrown in case of trouble.
     */
    public CapabilitySet(byte[] codedForm) throws IOException {
        this(codedForm, 0, codedForm.length);
    }

    /**
     * Creates a capability set from a serialized form.
     *
     * @param codedForm contains the serialized form, as produced by the
     *                  {@link #toBytes()} method.
     * @param offset    offset where input for this method starts.
     * @param length    length of input for this method.
     * @exception IOException is thrown in case of trouble.
     */
    public CapabilitySet(byte[] codedForm, int offset, int length) throws IOException {
        this(new DataInputStream(new ByteArrayInputStream(codedForm, offset, length)));
    }

    /**
     * Creates a capability set by reading it from the specified input stream.
     *
     * @param dis the input stream to read from.
     * @exception IOException is thrown in case of trouble.
     */
    public CapabilitySet(DataInput dis) throws IOException {
        doRead(dis);
        codedForm = computeCodedForm();
    }

    /**
     * Returns the capability set represented as a byte array.
     *
     * @return the byte array.
     */
    public byte[] toBytes() {
        return codedForm.clone();
    }

    private byte[] computeCodedForm() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            doWrite(dos);
            dos.close();
            return bos.toByteArray();
        } catch (Exception e) {
            // Should not happen. Ignored.
            return new byte[0];
        }
    }

    /**
     * Writes the capability set to the specified output stream.
     *
     * @param dataOutput the output stream to write to.
     * @exception IOException is thrown in case of trouble.
     */
    public void writeTo(DataOutput dataOutput) throws IOException {
        dataOutput.write(codedForm);
    }

    /**
     * Returns true if the specified capability is set.
     *
     * @param capability the specified capability.
     * @return true if the capability is set.
     */
    public boolean hasCapability(String capability) {
        return capabilities.contains(capability.toLowerCase());
    }

    /**
     * Matches the current capabilities with the capabilities supplied. We have a
     * match if the current capabilities are a subset of the specified capabilities.
     *
     * @param capabilitySet the capabilities to be matched with.
     * @return <code>true</code> if we have a match, <code>false</code> otherwise.
     */
    public boolean matchCapabilities(CapabilitySet capabilitySet) {
        for (String capability : capabilities) {
            if (!capabilitySet.hasCapability(capability)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the subset of capabilities that start with the specified prefix.
     *
     * @param prefix the specified prefix.
     * @return the capabilities that start with the specified prefix.
     */
    public CapabilitySet getCapabilitiesWithPrefix(String prefix) {
        CapabilitySet result = new CapabilitySet();
        for (String capability : capabilities) {
            if (capability.startsWith(prefix)) {
                result.capabilities.add(capability);
            }
        }
        result.codedForm = computeCodedForm();
        return result;
    }

    /**
     * Matches the current capabilities with the capabilities supplied. Returns a
     * capability set with the unmatched capabilities.
     *
     * @param capabilitySet the capabilities to be matched with.
     * @return the capabilities that don't match.
     */
    public CapabilitySet unmatchedCapabilities(CapabilitySet capabilitySet) {
        CapabilitySet result = new CapabilitySet();
        for (String capability : capabilities) {
            if (!capabilitySet.hasCapability(capability)) {
                result.capabilities.add(capability);
            }
        }
        result.codedForm = computeCodedForm();
        return result;
    }

    /**
     * Computes and returns the number of entries in this capability set.
     *
     * @return the number of entries.
     */
    public int size() {
        return capabilities.size();
    }

    /**
     * Reads and returns the capabilities from the specified file name, which is
     * searched for in the classpath.
     *
     * @param name the file name.
     * @return the capabilities from the specified file.
     * @exception IOException is thrown when an IO error occurs.
     */
    public static CapabilitySet load(String name) throws IOException {
        InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
        if (in == null) {
            throw new IOException("Could not open " + name);
        }
        return load(in);
    }

    /**
     * Reads and returns the capabilities from the specified input stream.
     *
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
     *
     * @param other the object to compare with.
     * @return <code>true</code> if equal, <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if ((other == null) || !(other instanceof CapabilitySet)) {
            return false;
        }

        CapabilitySet capabilitySet = (CapabilitySet) other;
        return capabilities.equals(capabilitySet.capabilities);
    }

    /**
     * Returns the hashcode of this capability set.
     *
     * @return the hashcode.
     */
    @Override
    public int hashCode() {
        return capabilities.hashCode();
    }

    private void writeObject(ObjectOutputStream output) throws IOException {
        doWrite(output);
    }

    private void doWrite(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(capabilities.size());
        for (String capability : capabilities) {
            dataOutput.writeUTF(capability);
        }
    }

    private void readObject(ObjectInputStream input) throws IOException {
        doRead(input);
    }

    private void doRead(DataInput dataInput) throws IOException {
        int size = dataInput.readInt();
        for (int i = 0; i < size; i++) {
            capabilities.add(dataInput.readUTF());
        }
        codedForm = computeCodedForm();
    }

    /**
     * Returns a string representation of this capability.
     *
     * @return a string representation.
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        String append = "";
        for (String capability : capabilities) {
            result.append(append);
            result.append(capability);
            append = " ";
        }
        return result.toString();
    }

    /**
     * Returns all the capabilities in this capability set as a String array, one
     * element per capability.
     *
     * @return the capabilities.
     */
    public String[] getCapabilities() {
        int size = capabilities.size();
        String[] result = new String[size];
        int i = 0;
        for (String capability : capabilities) {
            result[i] = capability;
            i++;
        }
        return result;
    }
}
