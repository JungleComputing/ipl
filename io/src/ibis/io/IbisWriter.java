package ibis.io;

import java.io.IOException;

/**
 * Abstract class that implements object writes for any kind of object.
 * The idea is that there is a separate writer for each kind of object,
 * so that runtime tests can be avoided.
 */
abstract class IbisWriter {
    abstract void writeObject(IbisSerializationOutputStream out, Object ref,
            AlternativeTypeInfo t, int hashCode, boolean unshared)
            throws IOException;

    void writeHeader(IbisSerializationOutputStream out, Object ref,
            AlternativeTypeInfo t, int hashCode, boolean unshared)
            throws IOException {
        // Code needed for most IbisWriters.
        if (! unshared) {
            out.assignHandle(ref, hashCode);
        }
        out.writeType(t.clazz);
        IbisSerializationOutputStream.addStatSendObject(ref);
    }
}
