/* $Id: IbisIdentifier.java 4893 2006-12-08 15:15:12Z ceriel $ */

package ibis.impl;

import ibis.io.Conversion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * This implementation of the {@link ibis.ipl.IbisIdentifier} interface
 * identifies an Ibis instance on the network.
 */
public final class IbisIdentifier implements ibis.ipl.IbisIdentifier {
    /**
     * This field is used to indicate to which virtual or physical cluster
     * this ibis belongs.
     */
    public final String cluster;

    /** Hostname on which this Ibis instance runs. */
    public final String host;

    /** Numbering of Ibis instances, provided by the registry. */
    public final int joinId;

    /** The name of the pool to which this Ibis instance belongs. */
    public final String pool;

    /** Extra data for implementation. */
    private final byte[] data;

    /** An Ibis identifier coded as a byte array. Computed once. */
    private transient byte[] codedForm = null;

    /**
     * Constructs an <code>IbisIdentifier</code> with the specified parameters.
     * @param id join number, allocated by the registry.
     * @param data implementation-dependent data.
     * @param cluster cluster to which this ibis instance belongs.
     * @param pool identifies the run with the registry.
     * @param host host name on which this instance runs.
     */
    public IbisIdentifier(int id, byte[] data, String cluster,
            String pool, String host) {
        this.joinId = id;
        this.data = data;
        this.cluster = cluster;
        this.pool = pool;
        this.host = host;
    }

    /**
     * Constructs an <code>IbisIdentifier</code> from the specified coded form.
     * @param codedForm the coded form.
     */
    public IbisIdentifier(byte[] codedForm) {
        this(codedForm, 0, codedForm.length);
        this.codedForm = codedForm;
    }

    /**
     * Constructs an <code>IbisIdentifier</code> from the specified coded form,
     * at a particular offset and size.
     * @param codedForm the coded form.
     * @param offset offset in the coded form.
     * @param size size of the coded form.
     */
    public IbisIdentifier(byte[] codedForm, int offset, int size) {
        int clusterSize = Conversion.defaultConversion.byte2int(codedForm,
                offset);
        int hostSize = Conversion.defaultConversion.byte2int(codedForm,
                offset + Conversion.INT_SIZE);
        int poolSize = Conversion.defaultConversion.byte2int(codedForm,
                offset + 2*Conversion.INT_SIZE);
        int dataSize = Conversion.defaultConversion.byte2int(codedForm,
                offset + 3*Conversion.INT_SIZE);
        joinId = Conversion.defaultConversion.byte2int(codedForm,
                offset + 4*Conversion.INT_SIZE);
        offset += 5*Conversion.INT_SIZE;
        cluster = new String(codedForm, offset, clusterSize);
        offset += clusterSize;
        host = new String(codedForm, offset, hostSize);
        offset += hostSize;
        pool = new String(codedForm, offset, poolSize);
        offset += poolSize;
        data = new byte[dataSize];
        System.arraycopy(codedForm, offset, data, 0, dataSize);
        // size should now be equal offset + dataSize.
    }

    /**
     * Computes the coded form of this <code>IbisIdentifier</code>.
     * @return the coded form.
     */
    public byte[] getBytes() {
        if (codedForm == null) {
            byte[] clusterBytes = cluster.getBytes();
            byte[] hostBytes = host.getBytes();
            byte[] poolBytes = pool.getBytes();
            int sz = clusterBytes.length + hostBytes.length + poolBytes.length
                + data.length + 5 * Conversion.INT_SIZE;
            codedForm = new byte[sz];
            Conversion.defaultConversion.int2byte(clusterBytes.length,
                    codedForm, 0);
            Conversion.defaultConversion.int2byte(hostBytes.length,
                    codedForm, Conversion.INT_SIZE);
            Conversion.defaultConversion.int2byte(poolBytes.length,
                    codedForm, 2*Conversion.INT_SIZE);
            Conversion.defaultConversion.int2byte(data.length,
                    codedForm, 3*Conversion.INT_SIZE);
            Conversion.defaultConversion.int2byte(joinId,
                    codedForm, 4*Conversion.INT_SIZE);
            int offset = 5*Conversion.INT_SIZE;
            System.arraycopy(clusterBytes, 0, codedForm, offset,
                    clusterBytes.length);
            offset += clusterBytes.length;
            System.arraycopy(hostBytes, 0, codedForm, offset, hostBytes.length);
            offset += hostBytes.length;
            System.arraycopy(poolBytes, 0, codedForm, offset, poolBytes.length);
            offset += poolBytes.length;
            System.arraycopy(data, 0, codedForm, offset, data.length);
        }
        return codedForm;
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
        return other.joinId == joinId && other.pool.equals(pool);
    }

    /**
     * Computes the hashcode.
     * @return the hashcode.
     */
    public int hashCode() {
        return joinId;
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
        return "(Ibis on " + host + ", pool " + pool + ", id " + joinId + ")";
    }

    public String cluster() {
        return cluster;
    }

    public String getPool() {
        return pool;
    }

    /**
     * Obtains the implementation dependent data.
     * @return the data.
     */
    public byte[] getData() {
        return data;
    }
}
