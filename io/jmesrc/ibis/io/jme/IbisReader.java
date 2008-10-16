package ibis.io.jme;

import java.io.IOException;

/**
 * The interface for reading objects.
 * The idea is that there is a separate reader for each kind of object,
 * so that runtime tests can be avoided.
 */
interface IbisReader {
    Object readObject(ObjectInputStream in,
            AlternativeTypeInfo t, int typeHandle)
            throws IOException, ClassNotFoundException;
}
