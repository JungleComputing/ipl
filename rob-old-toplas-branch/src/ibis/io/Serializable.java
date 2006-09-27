/* $Id$ */

package ibis.io;

import java.io.IOException;

/**
 * This is the interface implemented by classes that have been rewritten using
 * IOGenerator. In addition, IOGenerator usually also generates a constructor
 * which reads the object from a <code>IbisSerializationInputStream</code>.
 */
public interface Serializable {
    /**
     * Takes care of writing the object, including parent objects, to the
     * <code>IbisSerializationOutputStream</code> parameter.
     * @param out the <code>IbisSerializationOutputStream</code> to which the
     * 		  object is written
     * @exception IOException is thrown when an IO error occurs. 	
     */
    public void generated_WriteObject(IbisSerializationOutputStream out)
            throws IOException;

    /**
     * Writes the serializable fields at level <code>lvl</code> of this object.
     * The "level" of an object is determined as follows:<ul>
     * <li> if its superclass is serializable: the level of the superclass + 1.
     * <li> if not: 1.
     * </ul>
     *
     * @param out the <code>IbisSerializationOutputStream</code> to which the
     * 		  fields are written
     * @param lvl the "level" of the fields written
     * @exception IOException is thrown when an IO error occurs. 	
     */
    public void generated_DefaultWriteObject(IbisSerializationOutputStream out,
            int lvl) throws IOException;

    /**
     * Reads the serializable fields at level <code>lvl</code> of this object.
     * The "level" of an object is determined as follows:<ul>
     * <li> if its superclass is serializable: the level of the superclass + 1.
     * <li> if not: 1.
     * </ul>
     *
     * @param in  the <code>IbisSerializationInputStream</code> from which the
     * 		  fields are read
     * @param lvl the "level" of the fields read
     * @exception IOException is thrown when an IO error occurs. 	
     */
    public void generated_DefaultReadObject(IbisSerializationInputStream in,
            int lvl) throws IOException, ClassNotFoundException;
}
