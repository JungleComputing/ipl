package ibis.io.jme;

import java.io.IOException;

/**
 * This interface replaces the java.io.Externalizable interface
 * 
 * @author Nick Palmer (npr200@few.vu.nl)
 *
 */
public interface Externalizable extends ibis.io.jme.Serializable {
	/** 
	 * The object implements the readExternal method to restore its contents by calling 
	 * the methods of DataInput for primitive types and readObject for objects, strings 
	 * and arrays.
	 * @param in the stream to read data from in order to restore the object
	 * @throws IOException if I/O errors occur
	 * @throws ClassNotFoundException If the class for an object being restored cannot be found.
	 */ 
	void readExternal(ObjectInput in) throws IOException, ClassNotFoundException;
    
	/**
	 * The object implements the writeExternal method to save its contents by calling 
	 * the methods of DataOutput for its primitive values or calling the writeObject 
	 * method of ObjectOutput for objects, strings, and arrays.
	 * @param out the stream to write the object to
	 * @throws IOException If any I/O exceptions occur
	 */
	void writeExternal(ObjectOutput out) throws IOException;
}
