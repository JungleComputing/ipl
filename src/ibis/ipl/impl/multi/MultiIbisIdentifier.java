/* $Id: IbisIdentifier.java 7294 2008-01-22 13:30:10Z ceriel $ */

package ibis.ipl.impl.multi;

import ibis.ipl.IbisIdentifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;

/**
 * This implementation of the {@link ibis.ipl.IbisIdentifier} interface
 * identifies an Ibis instance on the network.
 */
public final class MultiIbisIdentifier implements IbisIdentifier {
    /**
     * Generated
     */
    private static final long serialVersionUID = 5259510418367257559L;

    /**
     * The location for this Ibis instance. */
    public final Location location;

    /** The name of the pool to which this Ibis instance belongs. */
    public final String pool;

    /** Extra data for registry. */
    private byte[] registryData;

    /** Identification of Ibis instances, provided by the registry. */
    private final String id;

    /** An Ibis identifier coded as a byte array. Computed once. */
    private transient byte[] codedForm;

    private final HashMap<String, IbisIdentifier>idMap;

    /**
     * Constructs an <code>IbisIdentifier</code> with the specified parameters.
     * @param id join id, allocated by the registry.
     * @param idMap implementation-dependent data.
     * @param registryData registry-dependent data.
     * @param location location of this Ibis instance.
     * @param pool identifies the run with the registry.
     */
    public MultiIbisIdentifier(String id, HashMap<String, ibis.ipl.IbisIdentifier> idMap,
            byte[] registryData, Location location, String pool) {
        this.id = id;
        this.idMap = idMap;
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
    public MultiIbisIdentifier(byte[] codedForm) throws IOException {
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
    public MultiIbisIdentifier(byte[] codedForm, int offset, int size)
            throws IOException {
        this(new ObjectInputStream(
                new ByteArrayInputStream(codedForm, offset, size)));
    }

    /**
     * Reads an <code>IbisIdentifier</code> from the specified input stream.
     * @param dis the input stream.
     * @exception IOException is thrown in case of trouble.
     */
    public MultiIbisIdentifier(ObjectInputStream dis) throws IOException {
        location = new Location(dis);
        pool = dis.readUTF();
        idMap = new HashMap<String, ibis.ipl.IbisIdentifier>();
        int subCount = dis.readInt();
        for (int i = 0; i < subCount; i++) {
            try {
                String impl = dis.readUTF();
                ibis.ipl.IbisIdentifier ibisId = (ibis.ipl.IbisIdentifier)dis.readObject();
                idMap.put(impl, ibisId);
            } catch (ClassNotFoundException e) {
                // TODO should we be ignoring this?
                // It means we won't be able to connect on this Ibis but we lack
                // it anyway so I am not sure it matters.
            }
        }
        int registrySize = dis.readInt();
        if (registrySize < 0) {
            registryData = null;
        } else {
            registryData = new byte[registrySize];
            dis.readFully(registryData);
        }
        id = dis.readUTF();
        codedForm = computeCodedForm();
    }

    /**
     * Returns the coded form of this <code>IbisIdentifier</code>.
     * @return the coded form.
     */
    public byte[] toBytes() {
        if (codedForm == null) {
            codedForm = computeCodedForm();
        }
        return codedForm.clone();
    }

    private synchronized byte[] computeCodedForm() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream dos = new ObjectOutputStream(bos);
            location.writeTo(dos);
            dos.writeUTF(pool);
            dos.write(idMap.size());
            for (String impl:idMap.keySet()) {
                dos.writeUTF(impl);
                dos.writeObject(idMap.get(impl));
            }
            if (registryData == null) {
                dos.writeInt(-1);
            } else {
                dos.writeInt(registryData.length);
                dos.write(registryData);
            }
            dos.writeUTF(id);
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
        if (codedForm == null) {
            codedForm = computeCodedForm();
        }
        dos.write(codedForm);
    }

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

        MultiIbisIdentifier other = (MultiIbisIdentifier) o;
        return other.id.equals(id) && other.pool.equals(pool);
    }

    public int hashCode() {
        return id.hashCode();
    }

    public String toString() {
        return "(Ibis " + id + ", location " + location + ")";
    }

    public String name() {
        return "(Ibis " + id + ")";
    }

    public ibis.ipl.Location location() {
        return location;
    }

    public String poolName() {
        return pool;
    }

    /**
     * Obtains the registry dependent data.
     * @return the data.
     */
    public synchronized byte[] getRegistryData() {
        return registryData;
    }

    /**
     * Compare to the specified Ibis identifier.
     * @param c the Ibis identifier to compare to.
     */
    public int compareTo(ibis.ipl.IbisIdentifier c) {
        if (c instanceof MultiIbisIdentifier) {
            // If not, the specified Ibis identifier is from a completely
            // different implementation.
            MultiIbisIdentifier other = (MultiIbisIdentifier) c;
            // First compare pools.
            int cmp = pool.compareTo(other.pool);
            if (cmp == 0) {
                cmp = location.compareTo(other.location);
                if (cmp == 0) {
                    // Finally compare id.
                    return id.compareTo(other.id);
                }
            }
            return cmp;
        }
        return this.getClass().getName().compareTo(c.getClass().getName());
    }

    public String getID() {
        return id;
    }

    public IbisIdentifier subIdForIbis(String ibisName) {
        return idMap.get(ibisName);
    }
}
