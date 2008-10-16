package ibis.io.jme;

import java.io.IOException;

/**
 * The interface for writing objects.
 */
interface IbisWriter {
    void writeObject(ObjectOutputStream out, Object ref,
            AlternativeTypeInfo t, int hashCode, boolean unshared)
            throws IOException;

    void writeHeader(ObjectOutputStream out, Object ref,
            AlternativeTypeInfo t, int hashCode, boolean unshared)
            throws IOException;
}
