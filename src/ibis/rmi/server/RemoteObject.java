package ibis.rmi.server;

import ibis.rmi.MarshalException;
import ibis.rmi.Remote;
import ibis.rmi.UnmarshalException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * The <code>RemoteObject</code> class implements the <code>Object</code>
 * behaviour for remote objects, by implementing methods for
 * <code>hashCode</code>, <code>equals</code>, and <code>toString</code>.
 */
public abstract class RemoteObject implements Remote, Serializable {
    /** The remote reference. */
    transient protected RemoteRef ref;

    /**
     * Creates a <code>RemoteObject</code>.
     */
    protected RemoteObject() {
        this(null);
    }

    /**
     * Creates a <code>RemoteObject</code> with the specified reference.
     */
    protected RemoteObject(RemoteRef newref) {
        ref = newref;
    }

    /**
     * Returns a hashcode for the remote object.
     * @return the hashcode.
     */
    public int hashCode() {
        return (ref == null) ? super.hashCode() : ref.remoteHashCode();
    }

    /**
     * Compares the specified object with the remote object.
     * @return the result of the comparison
     * @param obj the object to compare with
     */
    public boolean equals(Object obj) {
        if (obj instanceof RemoteObject) {
            if (ref == null) {
                return obj == this;
            }
            return ref.remoteEquals(((RemoteObject) obj).ref);
        }
        if (obj != null) {
            return obj.equals(this);
        }
        return false;
    }

    /**
     * Returns a string representation of the value of this remote object.
     * @return the string representation for this remote object.
     */
    public String toString() {
        String classname = this.getClass().getName();
        String hc = "@" + Integer.toHexString(System.identityHashCode(this));
        return (ref == null) ? classname + hc : classname + hc + "["
                + ref.remoteToString() + "]";
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (ref == null) {
            throw new MarshalException("no ref to serialize");
        }
        out.writeUTF(ref.getRefClass(out));
        ref.writeExternal(out);

    }

    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        String name = "ibis.rmi.impl." + in.readUTF();
        try {
            Class cls = Class.forName(name);
            ref = (RemoteRef) cls.newInstance();
            ref.readExternal(in);
        } catch (InstantiationException e) {
            throw new UnmarshalException("failed to instantiate ref");
        } catch (IllegalAccessException e) {
            throw new UnmarshalException("failed to create ref");
        }
    }

}