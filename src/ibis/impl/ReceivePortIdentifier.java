/* $Id: ReceivePortIdentifier.java 4910 2006-12-13 09:01:33Z ceriel $ */

package ibis.impl;

import ibis.ipl.StaticProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Implementation of the <code>ReceivePortIdentifier</code> interface.
 * This class can be extended by Ibis implementations.
 */
public class ReceivePortIdentifier implements ibis.ipl.ReceivePortIdentifier,
        java.io.Serializable {

    /** The name of the corresponding receiveport. */
    protected final String name;

    /** The properties of the corresponding receiveport. */
    protected final StaticProperties type;

    /** The IbisIdentifier of the Ibis instance that created the receiveport. */
    protected final IbisIdentifier ibis;

    /** A receiveport identifier coded as a byte array. Computed once. */
    protected transient byte[] codedForm = null;

    /**
     * Constructor, initializing the fields with the specified parameters.
     * @param name the name of the receiveport.
     * @param type the properties of the receiveport.
     * @param ibis the Ibis instance that created the receiveport.
     */
    public ReceivePortIdentifier(String name, StaticProperties type,
            IbisIdentifier ibis) {
        this.name = name;
        this.type = type;
        this.ibis = ibis;
    }

    /**
     * Constructs a <code>ReceivePortIdentifier</code> from the specified coded
     * form.
     * @param codedForm the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public ReceivePortIdentifier(byte[] codedForm) throws IOException {
        this(codedForm, 0, codedForm.length);
        this.codedForm = codedForm;
    }

    /**
     * Constructs a <code>ReceivePortIdentifier</code> from the specified coded
     * form, at a particular offset and size.
     * @param codedForm the coded form.
     * @param offset offset in the coded form.
     * @param length length of the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public ReceivePortIdentifier(byte[] codedForm, int offset, int length)
            throws IOException {
        this(new DataInputStream(
                new ByteArrayInputStream(codedForm, offset, length)));
    }

    /**
     * Constructs a <code>ReceivePortIdentifier</code> by reading it from the
     * specified input stream.
     * @param dis the input stream.
     * @exception IOException is thrown in case of trouble.
     */
    public ReceivePortIdentifier(DataInput dis) throws IOException {
        name = dis.readUTF();
        type = new StaticProperties(dis);
        ibis = new IbisIdentifier(dis);
    }

    /**
     * Computes the coded form of this <code>ReceivePortIdentifier</code>.
     * @return the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public byte[] toBytes() throws IOException {
        if (codedForm == null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(name);
            type.writeTo(dos);
            ibis.writeTo(dos);
            dos.flush();
            codedForm = bos.toByteArray();
        }
        return codedForm;
    }

    /**
     * Writes this <code>ReceivePortIdentifier</code> to the specified output
     * stream.
     * @param dos the output stream.
     * @exception IOException is thrown in case of trouble.
     */
    public void writeTo(DataOutput dos) throws IOException {
        toBytes();
        dos.write(codedForm);
    }

    private boolean equals(ReceivePortIdentifier other) {
        if (other == this) {
            return true;
        }
        return name.equals(other.name) && ibis.equals(other.ibis);
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof ReceivePortIdentifier) {
            return equals((ReceivePortIdentifier) other);
        }
        return false;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String name() {
        return name;
    }

    public StaticProperties type() {
        return new StaticProperties(type);
    }

    public ibis.ipl.IbisIdentifier ibis() {
        return ibis;
    }

    public String toString() {
        return ("(ReceivePortIdentifier: name = " + name
                + ", ibis = " + ibis + ")");
    }
}
