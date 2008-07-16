/* $Id$ */

package ibis.io;

import java.io.IOException;

/**
 * The <code>Generator</code> class is the base class for IOGenerator-generated
 * classes that are generated for a single purpose: to have a method that
 * generates a new instance of an object by reading it from a
 * <code>IbisSerializationInputStream</code>.
 * To accomplish this, without using native code and without violating
 * the bytecode validation, the IOGenerator generates a constructor that
 * initializes the object from a <code>IbisSerializationInputStream</code>,
 * and a separate class that contains a method invoking this constructor
 * and returning its result.
 */
public abstract class Generator {
    public abstract Object generated_newInstance(
            IbisSerializationInputStream in)
                    throws IOException, ClassNotFoundException;
}
