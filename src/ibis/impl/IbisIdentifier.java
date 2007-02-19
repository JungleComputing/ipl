/* $Id$ */

package ibis.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This implementation of the {@link ibis.ipl.IbisIdentifier} interface
 * identifies an Ibis instance on the network.
 */
public final class IbisIdentifier implements ibis.ipl.IbisIdentifier {
    /**
     * The location for this Ibis instance. */
    public final Location location;

    /** The name of the pool to which this Ibis instance belongs. */
    public final String pool;

    /** Extra data for implementation. */
    private final byte[] implementationData;

    /** Extra data for registry. */
    private final byte[] registryData;

    /** Identification of Ibis instances, provided by the registry. */
    public final String myId;

    /** An Ibis identifier coded as a byte array. Computed once. */
    private final transient byte[] codedForm;

    /**
     * Constructs an <code>IbisIdentifier</code> with the specified parameters.
     * @param id join id, allocated by the registry.
     * @param implementationData implementation-dependent data.
     * @param registryData registry-dependent data.
     * @param location location of this Ibis instance.
     * @param pool identifies the run with the registry.
     */
    public IbisIdentifier(String id, byte[] implementationData,
            byte[] registryData, Location location, String pool) {
        this.myId = id;
        this.implementationData = implementationData;
        this.registryData = registryData;
        this.location = location;
        this.pool = pool;
        this.codedForm = computeCodedForm();
    }

    /**
     * Constructs an <code>IbisIdentifier</code> from the specified coded form.
     * @param codedForm the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public IbisIdentifier(byte[] codedForm) throws IOException {
        this(codedForm, 0, codedForm.length);
    }

    /**
     * Constructs an <code>IbisIdentifier</code> from the specified coded form,
     * at a particular offset and size.
     * @param codedForm the coded form.
     * @param offset offset in the coded form.
     * @param size size of the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public IbisIdentifier(byte[] codedForm, int offset, int size)
            throws IOException {
        this(new DataInputStream(
                new ByteArrayInputStream(codedForm, offset, size)));
    }

    /**
     * Reads an <code>IbisIdentifier</code> from the specified input stream.
     * @param dis the input stream.
     * @exception IOException is thrown in case of trouble.
     */
    public IbisIdentifier(DataInput dis) throws IOException {
        location = new Location(dis);
        pool = dis.readUTF();
        int implementationSize = dis.readInt();
        if (implementationSize < 0) {
            implementationData = null;
        } else {
            implementationData = new byte[implementationSize];
            dis.readFully(implementationData);
        }
        int registrySize = dis.readInt();
        if (registrySize < 0) {
            registryData = null;
        } else {
            registryData = new byte[registrySize];
            dis.readFully(registryData);
        }
        myId = dis.readUTF();
        codedForm = computeCodedForm();
    }

    /**
     * Returns the coded form of this <code>IbisIdentifier</code>.
     * @return the coded form.
     */
    public byte[] toBytes() {
        return codedForm;
    }

    private byte[] computeCodedForm() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            location.writeTo(dos);
            dos.writeUTF(pool);
            if (implementationData == null) {
                dos.writeInt(-1);
            } else {
                dos.writeInt(implementationData.length);
                dos.write(implementationData);
            }
            if (registryData == null) {
                dos.writeInt(-1);
            } else {
                dos.writeInt(registryData.length);
                dos.write(registryData);
            }
            dos.writeUTF(myId);
            dos.close();
            return bos.toByteArray();
        } catch(Exception e) {
            // Should not happen.
            return null;
        }
    }

    /**
     * Adds coded form of this <code>IbisIdentifier</code> to the specified
     * output stream.
     * @param dos the output stream.
     * @exception IOException is thrown in case of trouble.
     */
    public void writeTo(DataOutput dos) throws IOException {
        dos.write(codedForm);
    }

    /**
     * Compares two Ibis identifiers.
     * @return <code>true</code> if equal, <code>false</code> if not.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }

        if (! o.getClass().equals(getClass())) {
            return false;
        }

        IbisIdentifier other = (IbisIdentifier) o;
        return other.myId.equals(myId) && other.pool.equals(pool);
    }

    /**
     * Computes the hashcode.
     * @return the hashcode.
     */
    public int hashCode() {
        return myId.hashCode();
    }

    /**
     * @return a string representation of this IbisIdentifier.
     */
    public String toString() {
        return "(Ibis " + myId + ", pool " + pool + ")";
    }

    public ibis.ipl.Location getLocation() {
        return location;
    }

    public String getPool() {
        return pool;
    }

    /**
     * Obtains the implementation dependent data.
     * @return the data.
     */
    public byte[] getImplementationData() {
        return implementationData;
    }

    /**
     * Obtains the registry dependent data.
     * @return the data.
     */
    public byte[] getRegistryData() {
        return registryData;
    }

    public String cluster() {
        String retval = "";
        int n = location.levels() - 1;
        for (int i = 0; i < n; i++) {
            if (i != 0) {
                retval += Location.SEPARATOR;
            }
            retval += location.levelName(i);
        }
        return retval;
    }

    /**
     * Compare to the specified Ibis identifier.
     * @param c the Ibis identifier to compare to.
     */
    public int compareTo(ibis.ipl.IbisIdentifier c) {
        if (c instanceof IbisIdentifier) {
            // If not, the specified Ibis identifier is from a completely
            // different implementation.
            IbisIdentifier id = (IbisIdentifier) c;
            // First compare pools.
            int cmp = pool.compareTo(id.pool);
            if (cmp == 0) {
                cmp = location.compareTo(id.location);
                if (cmp == 0) {
                    // Finally compare id.
                    return myId.compareTo(id.myId);
                }
            }
            return cmp;
        }
        return this.getClass().getName().compareTo(c.getClass().getName());
    }
}
