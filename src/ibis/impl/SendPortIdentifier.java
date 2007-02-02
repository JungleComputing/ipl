/* $Id: SendPortIdentifier.java 4910 2006-12-13 09:01:33Z ceriel $ */

package ibis.impl;

import ibis.io.Conversion;
import ibis.ipl.StaticProperties;

import java.io.IOException;

/**
 * Implementation of the {@link ibis.ipl.SendPortIdentifier} interface.
 * This class can be extended by Ibis implementations.
 */
public class SendPortIdentifier implements ibis.ipl.SendPortIdentifier,
        java.io.Serializable {

    /** The name of the corresponding sendport. */
    protected final String name;

    /** The properties of the corresponding sendport. */
    protected final StaticProperties type;

    /** The IbisIdentifier of the Ibis instance that created the sendport. */
    protected final IbisIdentifier ibis;

    /** Coded form, computed only once. */    
    protected transient byte[] codedForm = null;

    /**
     * Constructor, initializing the fields with the specified parameters.
     * @param name the name of the sendport.
     * @param type the properties of the sendport.
     * @param ibis the Ibis instance that created the sendport.
     */
    public SendPortIdentifier(String name, StaticProperties type,
            IbisIdentifier ibis) {
        this.name = name;
        this.type = type;
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
        this.codedForm = codedForm;
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
        int nameSize = Conversion.defaultConversion.byte2int(codedForm, offset);
        int typeSize = Conversion.defaultConversion.byte2int(codedForm,
                offset + Conversion.INT_SIZE);
        int ibisSize = Conversion.defaultConversion.byte2int(codedForm,
                offset + 2*Conversion.INT_SIZE);
        offset += 3*Conversion.INT_SIZE;
        name = new String(codedForm, offset, nameSize);
        offset += nameSize;
        type = new StaticProperties(codedForm, offset, typeSize);
        offset += typeSize;
        ibis = new IbisIdentifier(codedForm, offset, ibisSize);
    }

    /**
     * Computes the coded form of this <code>SendPortIdentifier</code>.
     * @return the coded form.
     * @exception IOException is thrown in case of trouble.
     */
    public byte[] getBytes() throws IOException {
        if (codedForm == null) {
            byte[] nameBytes = name.getBytes();
            byte[] typeBytes = type.getBytes();
            byte[] ibisBytes = ibis.getBytes();
            int sz = nameBytes.length + typeBytes.length + ibisBytes.length
                + 3 * Conversion.INT_SIZE;
            codedForm = new byte[sz];
            Conversion.defaultConversion.int2byte(nameBytes.length,
                    codedForm, 0);
            Conversion.defaultConversion.int2byte(typeBytes.length,
                    codedForm, Conversion.INT_SIZE);
            Conversion.defaultConversion.int2byte(ibisBytes.length,
                    codedForm, 2*Conversion.INT_SIZE);
            int offset = 3*Conversion.INT_SIZE;
            System.arraycopy(nameBytes, 0, codedForm, offset, nameBytes.length);
            offset += nameBytes.length;
            System.arraycopy(typeBytes, 0, codedForm, offset, typeBytes.length);
            offset += typeBytes.length;
            System.arraycopy(ibisBytes, 0, codedForm, offset, ibisBytes.length);
        }
        return codedForm;
    }

    private boolean equals(SendPortIdentifier other) {
        if (other == this) {
            return true;
        }
        return name.equals(other.name) && ibis.equals(other.ibis)
                && type.equals(other.type);
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
        return name.hashCode();
    }

    public final String name() {
        return name;
    }

    public final StaticProperties type() {
        return new StaticProperties(type);
    }

    public ibis.ipl.IbisIdentifier ibis() {
        return ibis;
    }

    public String toString() {
        return ("(SendPortIdentifier: name = " + name + ", type = " + type
                + ", ibis = " + ibis + ")");
    }
}
