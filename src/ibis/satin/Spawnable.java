package ibis.satin;

/**
 * The marker interface that indicates that a class contains a method that is
 * spawnable by the Satin divide-and-conquer environment. Use this interface to
 * mark the methods that may be spawned. The way to do this is to create an
 * interface that extends <code>ibis.satin.Spawnable</code> and specifies the
 * methods that may be spawned. The interface extends java.io.Serializable
 * because the "this" parameter is also sent across the network when work is
 * transferred.
 */

public interface Spawnable extends java.io.Serializable {
	// just a marker interface
}