/* $Id$ */

package ibis.frontend;

import java.io.IOException;
import java.io.InputStream;

/**
 * This interface serves as a wrapper interface for different bytecode
 * rewriters. 
 * This way, different <code>IbiscComponents</code> can use different bytecode
 * rewriters. For each bytecode rewriter, the methods in this interface
 * must be implemented, as well as the methods in <code>ClassInfo</code>.
 */
public interface ByteCodeWrapper {
    /**
     * Obtains the <code>ClassInfo</code> object that encapsulates the
     * parameter, which represents a class for a specific bytecode rewriter.
     * @param cl the class as represented by a specific bytecode rewriter.
     * @return the corresponding <code>ClassInfo</code> object.
     */
    public ClassInfo getInfo(Object cl);

    /**
     * Reads a class from the specified file and returns the corresponding
     * <code>ClassInfo</code> object.
     * @param fileName the name of the file to read the class from.
     * @exception IOException is thrown on a read error.
     * @return the corresponding <code>ClassInfo</code> object.
     */
    public ClassInfo parseClassFile(String fileName) throws IOException;

    /**
     * Reads a class from the specified input stream and returns the
     * corresponding <code>ClassInfo</code> object.
     * @param in the input stream to read the class from.
     * @param fileName the name of the file/jar entry.
     * @exception IOException is thrown on a read error.
     * @return the corresponding <code>ClassInfo</code> object.
     */
    public ClassInfo parseInputStream(InputStream in, String fileName)
            throws IOException;
}
