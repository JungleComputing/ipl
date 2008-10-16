package ibis.io.jme;

import java.io.IOException;

public abstract class IbisReaderWriter implements IbisReader, IbisWriter {
    public void writeHeader(ObjectOutputStream out, Object ref,
            AlternativeTypeInfo t, int hashCode, boolean unshared)
            throws IOException {
        // Code needed for most IbisWriters.
        if (! unshared) {
            out.assignHandle(ref, hashCode);
        }
        out.writeType(t.clazz);
        ObjectOutputStream.addStatSendObject(ref);
    }
}
