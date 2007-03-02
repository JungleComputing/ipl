package ibis.io;

import java.io.IOException;

/**
 * Abstract class that implements object writes for any kind of object.
 * The idea is that there is a separate writer for each kind of object,
 * so that runtime tests can be avoided.
 */
abstract class IbisWriter {
    abstract void writeObject(IbisSerializationOutputStream out, Object ref,
            Class clazz, int hashCode, boolean unshared) throws IOException;
}
