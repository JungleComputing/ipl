/* $Id: IbisIdentifier.java 4893 2006-12-08 15:15:12Z ceriel $ */

package ibis.impl;

import ibis.io.Conversion;

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
     * This field is used to indicate to which virtual or physical cluster
     * this ibis belongs.
     */
    public final String[] cluster;

    /** The name of the pool to which this Ibis instance belongs. */
    public final String pool;

    /** Extra data for implementation. */
    private final byte[] implementationData;

    /** Extra data for registry. */
    private final byte[] registryData;

    /** Identification of Ibis instances, provided by the registry. */
    public final String myId;

    /** An Ibis identifier coded as a byte array. Computed once. */
    private transient byte[] codedForm = null;

    /**
     * Constructs an <code>IbisIdentifier</code> with the specified parameters.
     * @param id join id, allocated by the registry.
     * @param implementationData implementation-dependent data.
     * @param registryData registry-dependent data.
     * @param cluster cluster to which this ibis instance belongs.
     * @param pool identifies the run with the registry.
     */
    public IbisIdentifier(String id, byte[] implementationData,
            byte[] registryData, String[] cluster, String pool) {
        this.myId = id;
        this.implementationData = implementationData;
        this.registryData = registryData;
        this.cluster = cluster;
        this.pool = pool;
    }

    /**
     * Constructs an <code>IbisIdentifier</code> from the specified coded form.
     * @param codedForm the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public IbisIdentifier(byte[] codedForm) throws IOException {
        this(codedForm, 0, codedForm.length);
        this.codedForm = codedForm;
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
        int clusterSize = dis.readInt();
        if (clusterSize < 0) {
            cluster = null;
        } else {
            cluster = new String[clusterSize];
            for (int i = 0; i < clusterSize; i++) {
                cluster[i] = dis.readUTF();
            }
        }
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
    }

    /**
     * Computes the coded form of this <code>IbisIdentifier</code>.
     * @return the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public byte[] toBytes() throws IOException {
        if (codedForm == null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            if (cluster == null) {
                dos.writeInt(-1);
            } else {
                dos.writeInt(cluster.length);
                for (int i = 0; i < cluster.length; i++) {
                    dos.writeUTF(cluster[i]);
                }
            }
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
            codedForm = bos.toByteArray();
        }
        return codedForm;
    }

    /**
     * Adds coded form of this <code>IbisIdentifier</code> to the specified
     * output stream.
     * @param dos the output stream.
     * @exception IOException is thrown in case of trouble.
     */
    public void writeTo(DataOutput dos) throws IOException {
        toBytes();
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
     * Initializes the <code>cluster</code> field.
     */
    public static String getCluster() {
        String cluster = System.getProperty("ibis.pool.cluster");
        if (cluster == null) {
            // Backwards compatibility, will be deprecated.
            cluster = System.getProperty("cluster");
        }
        if (cluster == null) {
            cluster = "unknown";
        }
        return cluster;
    }

    /**
     * @return a string representation of this IbisIdentifier.
     */
    public String toString() {
        return "(Ibis " + myId + ", pool " + pool + ")";
    }

    public String cluster() {
        // Todo: convert ...
        if (cluster == null) {
            return null;
        }
        String str = "";
        for (int i = 0; i < cluster.length-1; i++) {
            str += "#" + cluster[i];
        }
        return str;
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
                // Then compare cluster.
                for (int i = 0; i < cluster.length; i++) {
                    if (i >= id.cluster.length) {
                        return -1;
                    }
                    cmp = cluster[i].compareTo(id.cluster[i]);
                    if (cmp != 0) {
                        return cmp;
                    }
                }
                if (id.cluster.length > cluster.length) {
                    return 1;
                }
                // Finally compare id.
                return myId.compareTo(id.myId);
            }
            return cmp;
        }
        return this.getClass().getName().compareTo(c.getClass().getName());
    }
}
