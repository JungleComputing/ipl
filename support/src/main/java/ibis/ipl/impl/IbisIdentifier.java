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

package ibis.ipl.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * This implementation of the {@link ibis.ipl.IbisIdentifier} interface
 * identifies an Ibis instance on the network.
 */
public final class IbisIdentifier implements ibis.ipl.IbisIdentifier {
    /**
     * Generated
     */
    private static final long serialVersionUID = 3654888190542934889L;

    /**
     * The location for this Ibis instance.
     */
    public final Location location;

    /** The name of the pool to which this Ibis instance belongs. */
    public final String pool;

    /** Extra data for implementation. */
    private final byte[] implementationData;

    /** Extra data for registry. */
    private final byte[] registryData;

    /** Identification of Ibis instances, provided by the registry. */
    private final String id;

    /** Application tag, provided by the application. */
    private final byte[] tag;

    /** An Ibis identifier coded as a byte array. Computed once. */
    private transient byte[] codedForm;

    /**
     * Constructs an <code>IbisIdentifier</code> with the specified parameters.
     * 
     * @param id
     *            join id, allocated by the registry.
     * @param implementationData
     *            implementation-dependent data.
     * @param registryData
     *            registry-dependent data.
     * @param location
     *            location of this Ibis instance.
     * @param pool
     *            identifies the run with the registry.
     * @param tag
     *            user-provided tag for this ibis.
     */
    public IbisIdentifier(String id, byte[] implementationData,
            byte[] registryData, Location location, String pool,
            byte[] tag) {
        this.id = id;
        this.implementationData = implementationData;
        this.registryData = registryData;
        this.location = location;
        this.pool = pool;
        this.tag = tag;
        this.codedForm = computeCodedForm();
    }

    /**
     * Constructs an <code>IbisIdentifier</code> from the specified coded form.
     * 
     * @param codedForm
     *            the coded form.
     * @exception IOException
     *                is thrown in case of trouble.
     */
    public IbisIdentifier(byte[] codedForm) throws IOException {
        this(codedForm, 0, codedForm.length);
    }

    /**
     * Constructs an <code>IbisIdentifier</code> from the specified coded form,
     * at a particular offset and size.
     * 
     * @param codedForm
     *            the coded form.
     * @param offset
     *            offset in the coded form.
     * @param size
     *            size of the coded form.
     * @exception IOException
     *                is thrown in case of trouble.
     */
    public IbisIdentifier(byte[] codedForm, int offset, int size)
            throws IOException {
        this(new DataInputStream(new ByteArrayInputStream(codedForm, offset,
                size)));
    }

    /**
     * Reads an <code>IbisIdentifier</code> from the specified input stream.
     * 
     * @param dis
     *            the input stream.
     * @exception IOException
     *                is thrown in case of trouble.
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
        id = dis.readUTF();
        int tagSize = dis.readInt();
        if (tagSize < 0) {
            tag = null;
        } else {
            tag = new byte[tagSize];
            dis.readFully(tag);
        }
        codedForm = computeCodedForm();
    }

    /**
     * Returns the coded form of this <code>IbisIdentifier</code>.
     * 
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
            dos.writeUTF(id);
            if (tag == null) {
                dos.writeInt(-1);
            } else {
                dos.writeInt(tag.length);
                dos.write(tag);
            }
            dos.close();
            return bos.toByteArray();
        } catch (Exception e) {
            // Should not happen.
            return null;
        }
    }

    /**
     * Adds coded form of this <code>IbisIdentifier</code> to the specified
     * output stream.
     * 
     * @param dos
     *            the output stream.
     * @exception IOException
     *                is thrown in case of trouble.
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

        if (!o.getClass().equals(getClass())) {
            return false;
        }

        IbisIdentifier other = (IbisIdentifier) o;
        return other.id.equals(id) && other.pool.equals(pool);
    }

    public int hashCode() {
        return id.hashCode();
    }

    public String toString() {
        return "(Ibis "
                + id
                + ", location "
                + location
                + ")";
    }

    public String name() {
        return "Ibis " + id;
    }

    public ibis.ipl.Location location() {
        return location;
    }

    public String poolName() {
        return pool;
    }

    public byte[] tag() {
        return tag;
    }

    public String tagAsString() {
        if (tag == null) {
            return null;
        }
        try {
            return new String(tag, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("could not convert tag to string", e);
        }
    }

    /**
     * Obtains the implementation dependent data.
     * 
     * @return the data.
     */
    public byte[] getImplementationData() {
        return implementationData;
    }

    /**
     * Obtains the registry dependent data.
     * 
     * @return the data.
     */
    public byte[] getRegistryData() {
        return registryData;
    }

    /**
     * Compare to the specified Ibis identifier.
     * 
     * @param c
     *            the Ibis identifier to compare to.
     */
    public int compareTo(ibis.ipl.IbisIdentifier c) {
        if (c instanceof IbisIdentifier) {
            // If not, the specified Ibis identifier is from a completely
            // different implementation.
            IbisIdentifier other = (IbisIdentifier) c;
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
}
