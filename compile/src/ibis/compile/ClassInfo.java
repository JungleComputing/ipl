/* $Id$ */

package ibis.compile;

import java.io.IOException;

/**
 * This interface is a wrapper for different bytecode rewriters, encapsulating
 * methods that apply to a specific class.
 * This way, different <code>IbiscComponents</code> can use different bytecode
 * rewriters. For each bytecode rewriter, the methods in this interface
 * must be implemented, as well as the methods in <code>ByteCodeWrapper</code>.
 */
public interface ClassInfo {
    /**
     * Obtains the name of the class.
     * @return the classname.
     */
    public String getClassName();

    /**
     * Obtains the object that represents this class for this specific
     * byte code rewriter. For instance, for BCEL, this is an object of
     * type <code>org.apache.bcel.classfile.JavaClass</code>.
     * @return the class object.
     */
    public Object getClassObject();
    
    /**
     * Writes the class to the specified filename.
     * @param fileName the name of the file to which the class is dumped.
     * @exception IOException is thrown when an error occurs during the write.
     */
    public void dump(String fileName) throws IOException;

    /**
     * Obtains the class as a byte array.
     * @return the byte array.
     */
    public byte[] getBytes();

    /**
     * Verifies the byte code, if the byte code rewriter supports it.
     * @return <code>true</code> if the verification succeeds or byte
     * code verification is not supported, <code>false</code> otherwise.
     */
    public boolean doVerify();
}
