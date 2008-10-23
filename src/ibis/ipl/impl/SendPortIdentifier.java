/* $Id$ */

package ibis.ipl.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Implementation of the {@link ibis.ipl.SendPortIdentifier} interface.
 * This class can be extended by Ibis implementations.
 */
public class SendPortIdentifier implements ibis.ipl.SendPortIdentifier {

    /** 
     * Generated
     */
    private static final long serialVersionUID = 8169019358172536222L;

    /** The name of the corresponding sendport. */
    public final String name;

    /** The IbisIdentifier of the Ibis instance that created the sendport. */
    public final IbisIdentifier ibis;

    /**
     * Constructor, initializing the fields with the specified parameters.
     * @param name the name of the sendport.
     * @param ibis the Ibis instance that created the sendport.
     */
    public SendPortIdentifier(String name, IbisIdentifier ibis) {
        if (name == null) {
            throw new NullPointerException("name is null in SendPortIdentifier");
        }
        if (ibis == null) {
            throw new NullPointerException("Ibis identifier is null in SendPortIdentifier");
        }
        this.name = name;
        this.ibis = ibis;
     }

    /**
     * Constructs a <code>SendPortIdentifier</code> from the specified coded
     * form.
     * @param codedForm the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public SendPortIdentifier(byte[] codedForm) throws IOException {
        this(codedForm, 0, codedForm.length);
    }

    /**
     * Constructs a <code>SendPortIdentifier</code> from the specified coded
     * form, at a particular offset and size.
     * @param codedForm the coded form.
     * @param offset offset in the coded form.
     * @param length length of the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public SendPortIdentifier(byte[] codedForm, int offset, int length)
            throws IOException {
        this(new DataInputStream(
                new ByteArrayInputStream(codedForm, offset, length)));
    }

    /**
     * Constructs a <code>SendPortIdentifier</code> by reading it from the
     * specified input stream.
     * @param dis the input stream.
     * @exception IOException is thrown in case of trouble.
     */
    public SendPortIdentifier(DataInput dis) throws IOException {
        name = dis.readUTF();
        ibis = new IbisIdentifier(dis);
    }

    /**
     * Returns the coded form of this <code>SendPortIdentifier</code>.
     * @return the coded form.
     */
    public byte[] toBytes() {
        return computeCodedForm();
    }

    private byte[] computeCodedForm() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(name);
            ibis.writeTo(dos);
            dos.close();
            return bos.toByteArray();
        } catch(Exception e) {
            // Should not happen. Ignored.
            return null;
        } 
    }

    /**
     * Writes this <code>SendPortIdentifier</code> to the specified output
     * stream.
     * @param dos the output stream.
     * @exception IOException is thrown in case of trouble.
     */
    public void writeTo(DataOutput dos) throws IOException {
         dos.write(computeCodedForm());
    }

    private boolean equals(SendPortIdentifier other) {
        if (other == this) {
            return true;
        }
        return name.equals(other.name) && ibis.equals(other.ibis);
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof SendPortIdentifier) {
            return equals((SendPortIdentifier) other);
        }
        return false;
    }

    public int hashCode() {
        return name.hashCode() ^ ibis.hashCode();
    }

    public final String name() {
        return name;
    }

    public ibis.ipl.IbisIdentifier ibisIdentifier() {
        return ibis;
    }

    public String toString() {
        return ("(SendPortIdentifier: name = \"" + name
                + "\", ibis = \"" + ibis + "\")");
    }
}
