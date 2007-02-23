/* $Id$ */

package ibis.ipl;

/**
 * Identifies a {@link ibis.ipl.SendPort SendPort} on the receiver side.
 */
public interface SendPortIdentifier extends java.io.Serializable {
    /**
     * Returns the name of the {@link ibis.ipl.SendPort SendPort}
     * corresponding to this identifier.
     * @return the name of the sendport.
     */
    public String name();

    /**
     * Returns the {@link ibis.ipl.IbisIdentifier IbisIdentifier} of the
     * {@link ibis.ipl.SendPort SendPort} corresponding to this identifier.
     * @return the ibis identifier.
     */
    public IbisIdentifier ibis();

    /**
     * The hashCode method is mentioned here just as a reminder that an
     * implementation must probably redefine it, because two objects
     * representing the same <code>SendPortIdentifier</code> must result
     * in the same hashcode (and compare equal).
     * To explicitly specify it in the interface does not help, because
     * java.lang.Object already implements it, but, anyway, here it is.
     */
    public int hashCode();

    /**
     * The equals method is mentioned here just as a reminder that an
     * implementation must probably redefine it, because two objects
     * representing the same <code>SendPortIdentifier</code> must
     * compare equal (and result in the same hashcode).
     * To explicitly specify it in the interface does not help, because
     * java.lang.Object already implements it, but, anyway, here it is.
     */
    public boolean equals(Object other);
}
